package io.scalaland.chimney.internal.compiletime

private[compiletime] trait ExprsPlatform extends Exprs { this: DefinitionsPlatform =>

  import c.universe.{internal as _, Transformer as _, *}

  final override protected type Expr[A] = c.Expr[A]
  protected object Expr extends ExprModule {

    object platformSpecific {

      def refineToLiteral[U: Type](literal: Type.Literal[U], tree: Tree, value: U): Expr[U] =
        if (isScala212) c.Expr[U](tree)
        else {
          val aType = literal(value).as_??
          import aType.Underlying as A
          c.Expr[A](tree).asInstanceOf[Expr[U]]
        }
    }
    import platformSpecific.*

    val Nothing: Expr[Nothing] = c.Expr[Nothing](q"???")
    val Null: Expr[Null] = c.Expr[Null](q"null")
    val Unit: Expr[Unit] = c.Expr[Unit](q"()")

    object Boolean extends BooleanModule {
      def apply(value: Boolean): Expr[Boolean] = refineToLiteral(Type.BooleanLiteral, q"$value", value)
    }
    object Int extends IntModule {
      def apply(value: Int): Expr[Int] = refineToLiteral(Type.IntLiteral, q"$value", value)
    }
    object Long extends LongModule {
      def apply(value: Long): Expr[Long] = refineToLiteral(Type.LongLiteral, q"$value", value)
    }
    object Float extends FloatModule {
      def apply(value: Float): Expr[Float] = refineToLiteral(Type.FloatLiteral, q"$value", value)
    }
    object Double extends DoubleModule {
      def apply(value: Double): Expr[Double] = refineToLiteral(Type.DoubleLiteral, q"$value", value)
    }
    object Char extends CharModule {
      def apply(value: Char): Expr[Char] = refineToLiteral(Type.CharLiteral, q"$value", value)
    }
    object String extends StringModule {
      def apply(value: String): Expr[String] = refineToLiteral(Type.StringLiteral, q"$value", value)
    }

    object Tuple2 extends Tuple2Module {
      def apply[A: Type, B: Type](a: Expr[A], b: Expr[B]): Expr[(A, B)] = c.Expr[(A, B)](q"($a, $b)")
    }

    object Function1 extends Function1Module {
      def apply[A: Type, B: Type](fn: Expr[A => B])(a: Expr[A]): Expr[B] = c.Expr[B](q"$fn.apply($a)")
    }
    object Function2 extends Function2Module {
      def tupled[A: Type, B: Type, C: Type](fn2: Expr[(A, B) => C]): Expr[((A, B)) => C] =
        c.Expr[((A, B)) => C](q"($fn2).tupled")
    }

    object Array extends ArrayModule {
      def apply[A: Type](args: Expr[A]*): Expr[Array[A]] =
        c.Expr[Array[A]](q"_root_.scala.Array[${Type[A]}](..$args)")

      def map[A: Type, B: Type](array: Expr[Array[A]])(fExpr: Expr[A => B]): Expr[Array[B]] =
        if (isScala212)
          c.Expr[Array[B]](q"$array.map[${Type[B]}, ${Type[Array[B]]}]($fExpr)")
        else
          c.Expr[Array[B]](q"$array.map[${Type[B]}]($fExpr)")

      def to[A: Type, C: Type](array: Expr[Array[A]])(
          factoryExpr: Expr[scala.collection.compat.Factory[A, C]]
      ): Expr[C] =
        // on Scala 2.12 .to(Factory[(k, v), M) creates... Iterable[(k, v)]
        if (isScala212)
          c.Expr[C](q"_root_.io.scalaland.chimney.integrations.FactoryCompat.arrayTo($array, $factoryExpr)")
        else c.Expr[C](q"$array.to($factoryExpr)")

      def iterator[A: Type](array: Expr[Array[A]]): Expr[Iterator[A]] = c.Expr[Iterator[A]](q"$array.iterator")
    }

    object Option extends OptionModule {
      def apply[A: Type](a: Expr[A]): Expr[Option[A]] = c.Expr[Option[A]](q"_root_.scala.Option[${Type[A]}]($a)")
      def empty[A: Type]: Expr[Option[A]] = c.Expr[Option[A]](q"_root_.scala.Option.empty[${Type[A]}]")
      val None: Expr[scala.None.type] = c.Expr[scala.None.type](q"_root_.scala.None")
      def map[A: Type, B: Type](opt: Expr[Option[A]])(f: Expr[A => B]): Expr[Option[B]] =
        c.Expr[Option[B]](q"$opt.map[${Type[B]}]($f)")
      def fold[A: Type, B: Type](opt: Expr[Option[A]])(onNone: Expr[B])(onSome: Expr[A => B]): Expr[B] =
        c.Expr[B](q"$opt.fold[${Type[B]}]($onNone)($onSome)")
      def orElse[A: Type](opt1: Expr[Option[A]], opt2: Expr[Option[A]]): Expr[Option[A]] =
        c.Expr[Option[A]](q"$opt1.orElse[${Type[A]}]($opt2)")
      def getOrElse[A: Type](opt: Expr[Option[A]])(orElse: Expr[A]): Expr[A] =
        c.Expr[A](q"$opt.getOrElse[${Type[A]}]($orElse)")
      def get[A: Type](opt: Expr[Option[A]]): Expr[A] =
        c.Expr[A](q"$opt.get")
      def isDefined[A: Type](opt: Expr[Option[A]]): Expr[Boolean] =
        c.Expr[Boolean](q"$opt.isDefined")
    }

    object Either extends EitherModule {
      def fold[L: Type, R: Type, A: Type](either: Expr[Either[L, R]])(left: Expr[L => A])(
          right: Expr[R => A]
      ): Expr[A] =
        c.Expr[A](q"""$either.fold[${Type[A]}]($left, $right)""")

      def orElse[L: Type, R: Type](either1: Expr[Either[L, R]], either2: Expr[Either[L, R]]): Expr[Either[L, R]] =
        if (isScala212)
          c.Expr[Either[L, R]](
            q"$either1.fold[_root_.scala.util.Either[${Type[L]}, ${Type[R]}]](_ => $either2, scala.util.Right(_))"
          )
        else c.Expr[Either[L, R]](q"$either1.orElse[${Type[L]}, ${Type[R]}]($either2)")

      object Left extends LeftModule {
        def apply[L: Type, R: Type](value: Expr[L]): Expr[Left[L, R]] =
          c.Expr[Left[L, R]](q"new _root_.scala.util.Left[${Type[L]}, ${Type[R]}]($value)")

        def value[L: Type, R: Type](left: Expr[Left[L, R]]): Expr[L] = c.Expr[L](q"$left.value")
      }
      object Right extends RightModule {
        def apply[L: Type, R: Type](value: Expr[R]): Expr[Right[L, R]] =
          c.Expr[Right[L, R]](q"new _root_.scala.util.Right[${Type[L]}, ${Type[R]}]($value)")

        def value[L: Type, R: Type](right: Expr[Right[L, R]]): Expr[R] = c.Expr[R](q"$right.value")
      }
    }

    object Iterable extends IterableModule {
      def map[A: Type, B: Type](iterable: Expr[Iterable[A]])(fExpr: Expr[A => B]): Expr[Iterable[B]] =
        c.Expr[Iterable[B]](q"$iterable.map[${Type[B]}]($fExpr)")

      def to[A: Type, C: Type](iterable: Expr[Iterable[A]])(
          factoryExpr: Expr[scala.collection.compat.Factory[A, C]]
      ): Expr[C] =
        // on Scala 2.12 .to(Factory[(k, v), M) creates... Iterable[(k, v)] and Factory[A, Hardcoded] creates... error
        if (isScala212)
          c.Expr[C](q"_root_.io.scalaland.chimney.integrations.FactoryCompat.iterableTo($iterable, $factoryExpr)")
        else c.Expr[C](q"$iterable.to($factoryExpr)")

      def iterator[A: Type](iterable: Expr[Iterable[A]]): Expr[Iterator[A]] = c.Expr[Iterator[A]](q"$iterable.iterator")
    }

    object Map extends MapModule {
      def iterator[K: Type, V: Type](map: Expr[scala.collection.Map[K, V]]): Expr[Iterator[(K, V)]] =
        c.Expr[Iterator[(K, V)]](q"$map.iterator")
    }

    object Iterator extends IteratorModule {
      def map[A: Type, B: Type](iterator: Expr[Iterator[A]])(fExpr: Expr[A => B]): Expr[Iterator[B]] =
        c.Expr[Iterator[B]](q"$iterator.map[${Type[B]}]($fExpr)")

      def concat[A: Type](iterator: Expr[Iterator[A]], iterator2: Expr[Iterator[A]]): Expr[Iterator[A]] =
        c.Expr[Iterator[A]](q"$iterator ++ $iterator2")

      def to[A: Type, C: Type](iterator: Expr[Iterator[A]])(
          factoryExpr: Expr[scala.collection.compat.Factory[A, C]]
      ): Expr[C] =
        // on Scala 2.12 .to(Factory[(k, v), M) creates... Iterable[(k, v)] and Factory[A, Hardcoded] creates... error
        if (isScala212)
          c.Expr[C](q"_root_.io.scalaland.chimney.integrations.FactoryCompat.iteratorTo($iterator, $factoryExpr)")
        else c.Expr[C](q"$iterator.to($factoryExpr)")

      def zipWithIndex[A: Type](it: Expr[Iterator[A]]): Expr[Iterator[(A, Int)]] =
        c.Expr[Iterator[(A, Int)]](q"$it.zipWithIndex")
    }

    def ifElse[A: Type](cond: Expr[Boolean])(ifBranch: Expr[A])(elseBranch: Expr[A]): Expr[A] =
      c.Expr[A](q"if ($cond) { $ifBranch } else { $elseBranch }")

    def block[A: Type](statements: List[Expr[Unit]], expr: Expr[A]): Expr[A] = c.Expr[A](q"..$statements; $expr")

    private type OptionExpr[A] = Option[Expr[A]]
    private val implicitCache = new Type.Cache[OptionExpr]
    def summonImplicit[A: Type]: Option[Expr[A]] = implicitCache(Type[A]) {
      scala.util
        .Try(c.inferImplicitValue(Type[A].tpe, silent = true, withMacrosDisabled = false))
        .toOption
        .filterNot(_ == EmptyTree)
        .map(c.Expr[A](_))
    }

    def nowarn[A: Type](warnings: Option[String])(expr: Expr[A]): Expr[A] = {
      val name = ExprPromise.platformSpecific.freshTermName("nowarnResult")
      c.Expr[A](
        warnings.fold(
          q"""
          @ _root_.scala.annotation.nowarn
          val $name = $expr
          $name
          """
        ) { msg =>
          q"""
          @ _root_.scala.annotation.nowarn($msg)
          val $name = $expr
          $name
          """
        }
      )
    }
    def SuppressWarnings[A: Type](warnings: List[String])(expr: Expr[A]): Expr[A] = {
      val name = ExprPromise.platformSpecific.freshTermName("suppressWarningsResult")
      c.Expr[A](
        q"""
        @ _root_.java.lang.SuppressWarnings(_root_.scala.Array(..$warnings))
        val $name = $expr
        $name
        """
      )
    }

    def suppressUnused[A: Type](expr: Expr[A]): Expr[Unit] =
      // In Scala 2.12 suppressing two variables at once resulted in "_ is already defined as value _" error
      if (isScala212) c.Expr[Unit](q"_root_.scala.Predef.locally { val _ = $expr }")
      else c.Expr[Unit](q"val _ = $expr")

    def eq[A: Type, B: Type](a: Expr[A], b: Expr[B]): Expr[Boolean] = c.Expr[Boolean](q"$a == $b")

    def asInstanceOf[A: Type, B: Type](expr: Expr[A]): Expr[B] = c.Expr[B](q"$expr.asInstanceOf[${Type[B]}]")

    def upcast[A: Type, B: Type](expr: Expr[A]): Expr[B] = {
      Predef.assert(
        Type[A] <:< Type[B],
        s"Upcasting can only be done to type proved to be super type! Failed ${Type.prettyPrint[A]} <:< ${Type.prettyPrint[B]} check"
      )
      if (Type[A] =:= Type[B]) expr.asInstanceOf[Expr[B]] // types are identical in practice, we can just cast
      else c.Expr[B](q"($expr : ${Type[B]})") // check A <:< B AND add a syntax to force upcasting
    }

    def prettyPrint[A](expr: Expr[A]): String =
      expr.tree
        .toString()
        // removes $macro$n from freshterms to make it easier to test and read
        .replaceAll("\\$macro", "")
        .replaceAll("\\$\\d+", "")
        // color expression for better UX (not as good as Scala 3 coloring but better than none)
        .split('\n')
        .map(line => Console.MAGENTA + line + Console.RESET)
        .mkString("\n")

    def typeOf[A](expr: Expr[A]): Type[A] = Type.platformSpecific.fromUntyped {
      try
        expr.actualType.finalResultType
      catch {
        case _: Throwable => expr.staticType.finalResultType
      }
    }
  }
}
