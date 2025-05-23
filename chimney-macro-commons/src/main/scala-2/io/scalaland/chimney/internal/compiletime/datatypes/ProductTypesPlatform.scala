package io.scalaland.chimney.internal.compiletime.datatypes

import io.scalaland.chimney.internal.compiletime.DefinitionsPlatform

import scala.collection.compat.*
import scala.collection.immutable.ListMap

trait ProductTypesPlatform extends ProductTypes { this: DefinitionsPlatform =>

  import c.universe.{internal as _, Transformer as _, *}

  protected object ProductType extends ProductTypesModule {

    object platformSpecific {

      def isParameterless(method: MethodSymbol): Boolean = method.paramLists.flatten.isEmpty

      def isDefaultConstructor(ctor: Symbol): Boolean =
        ctor != NoSymbol && ctor.isPublic && ctor.isConstructor && isParameterless(ctor.asMethod)

      def isAccessor(accessor: MethodSymbol): Boolean =
        accessor.isPublic && isParameterless(accessor)

      // assuming isAccessor was tested earlier
      def isCaseClassField(field: MethodSymbol): Boolean =
        field.isCaseAccessor || field.toString.startsWith("variable ") // align with Scala 3 (var -> case accessor)

      def isArgumentField(field: MethodSymbol): Boolean =
        isCaseClassField(field) || field.isParamAccessor

      def isBodyField(field: MethodSymbol): Boolean =
        field.isStable

      // assuming isAccessor was tested earlier
      def isJavaGetter(getter: MethodSymbol): Boolean =
        ProductTypes.BeanAware.isGetterName(getter.name.toString)

      def isJavaSetter(setter: MethodSymbol): Boolean =
        setter.isPublic && setter.paramLists.size == 1 && setter.paramLists.head.size == 1 &&
          ProductTypes.BeanAware.isSetterName(setter.asMethod.name.toString)

      def isVar(setter: Symbol): Boolean =
        setter.isPublic && setter.isTerm && setter.asTerm.name.toString.endsWith("_$eq")

      def isJavaSetterOrVar(setter: Symbol): Boolean =
        (setter.isMethod && isJavaSetter(setter.asMethod)) || isVar(setter)
    }

    import platformSpecific.*
    import Type.platformSpecific.*

    def isPOJO[A](implicit A: Type[A]): Boolean = {
      val sym = A.tpe.typeSymbol
      !A.isPrimitive && !(A <:< Type[String]) && !sym.isJavaEnum && sym.isClass && !sym.isAbstract &&
      publicPrimaryOrOnlyPublicConstructor(A.tpe).isDefined
    }
    def isCaseClass[A](implicit A: Type[A]): Boolean =
      isPOJO[A] && A.tpe.typeSymbol.asClass.isCaseClass
    def isCaseObject[A](implicit A: Type[A]): Boolean = {
      val sym = A.tpe.typeSymbol
      sym.isPublic && sym.isModuleClass && sym.asClass.isCaseClass
    }
    def isCaseVal[A](implicit A: Type[A]): Boolean = {
      val sym = A.tpe.typeSymbol
      sym.isPublic && sym.isModuleClass && sym.isStatic && sym.isFinal // parameterless case in S3 cannot be checked for "case"
    }
    def isJavaEnumValue[A](implicit A: Type[A]): Boolean = {
      val sym = A.tpe.typeSymbol
      sym.isPublic && sym.isJavaEnum && javaEnumRegexpFormat.pattern
        .matcher(A.tpe.toString)
        .matches() // 2.12 doesn't have .matches
    }
    def isJavaBean[A](implicit A: Type[A]): Boolean = {
      val mem = A.tpe.members
      isPOJO[A] && mem.exists(isDefaultConstructor) && mem.exists(isJavaSetterOrVar)
    }

    private type CachedExtraction[A] = Option[Product.Extraction[A]]
    private val extractionCache = new Type.Cache[CachedExtraction]
    def parseExtraction[A: Type]: Option[Product.Extraction[A]] = extractionCache(Type[A])(
      Some(
        Product.Extraction(
          ListMap.from[String, Existential[Product.Getter[A, *]]] {
            forceTypeSymbolInitialization[A]
            val localDefinitions = Type[A].tpe.decls.to(Set)
            Type[A].tpe.members.sorted
              .to(List)
              .filterNot(isGarbageSymbol)
              .collect { case method if method.isMethod => method.asMethod }
              .filter(isAccessor)
              .map { getter =>
                val name = getDecodedName(getter)
                val tpe = ExistentialType(fromUntyped(returnTypeOf(Type[A].tpe, getter)))
                import tpe.Underlying as Tpe
                def conformToIsGetters = !name.take(2).equalsIgnoreCase("is") || Tpe <:< Type[Boolean]
                name -> tpe.mapK[Product.Getter[A, *]] { implicit Tpe: Type[tpe.Underlying] => _ =>
                  val termName = getter.asMethod.name.toTermName
                  Product.Getter[A, Tpe](
                    sourceType =
                      if (isArgumentField(getter)) Product.Getter.SourceType.ConstructorArgVal
                      else if (isJavaGetter(getter) && conformToIsGetters) Product.Getter.SourceType.JavaBeanGetter
                      else if (isBodyField(getter)) Product.Getter.SourceType.ConstructorBodyVal
                      else Product.Getter.SourceType.AccessorMethod,
                    isInherited = !localDefinitions(getter),
                    get =
                      // TODO: handle pathological cases like getName[Unused]()()()
                      if (getter.asMethod.paramLists.isEmpty) (in: Expr[A]) => c.Expr[Tpe](q"$in.$termName")
                      else
                        (in: Expr[A]) =>
                          c.Expr[Tpe](q"$in.$termName(...${getter.paramLists.map(_.map(_.asInstanceOf[Tree]))})")
                  )
                }
              }
          }
        )
      )
    )

    private type CachedConstructor[A] = Option[Product.Constructor[A]]
    private val constructorCache = new Type.Cache[CachedConstructor]
    def parseConstructor[A: Type]: Option[Product.Constructor[A]] = constructorCache(Type[A]) {
      val A = Type[A].tpe
      val sym = A.typeSymbol
      forceTypeSymbolInitialization(sym)

      if (isJavaEnumValue[A]) {
        Some(Product.Constructor(ListMap.empty, _ => c.Expr[A](q"$A")))
      } else if (isCaseObject[A] || isCaseVal[A]) {
        Some(Product.Constructor(ListMap.empty, _ => c.Expr[A](q"${sym.asClass.module}")))
      } else if (isPOJO[A]) {
        val unambiguousConstructor = publicPrimaryOrOnlyPublicConstructor(A).getOrElse {
          // $COVERAGE-OFF$should never happen unless someone mess around with type-level representation
          assertionFailed(s"Expected public constructor of ${Type.prettyPrint[A]}")
          // $COVERAGE-ON$
        }
        val paramss = paramListsOf(A, unambiguousConstructor)
        val paramNames = paramss.flatMap(_.map(param => param -> getDecodedName(param))).toMap
        val paramTypes = paramsWithTypes(A, unambiguousConstructor)
        lazy val companion = companionSymbol[A]
        val defaultValues = paramss.flatten.zipWithIndex.collect {
          case (param, idx) if param.asTerm.isParamWithDefault =>
            val defaultIdx = idx + 1 // defaults are 1-indexed
            val scala2default = caseClassApplyDefaultScala2(defaultIdx)
            val scala3default = caseClassApplyDefaultScala3(defaultIdx)
            val newDefault = classNewDefaultScala2(defaultIdx)
            val defaults = List(scala2default, scala3default, newDefault)
            val foundDefault = companion.typeSignature.decls
              .to(List)
              .collectFirst {
                case method if defaults.contains(getDecodedName(method)) => method
              }
              .getOrElse {
                // $COVERAGE-OFF$should never happen unless someone mess around with type-level representation
                assertionFailed(
                  s"Expected that ${Type.prettyPrint[A]}'s constructor parameter `$param` would have default value: attempted `$scala2default`, `$scala3default` and `$newDefault`, found: ${companion.typeSignature.decls}"
                )
                // $COVERAGE-ON$
              }
            paramNames(param) -> q"$companion.$foundDefault"
        }.toMap
        val constructorParameters = ListMap.from(paramss.flatMap(_.map { param =>
          val name = paramNames(param)
          val tpe = ExistentialType(fromUntyped(paramTypes(name)))
          name ->
            tpe.mapK { implicit Tpe: Type[tpe.Underlying] => _ =>
              Product.Parameter(
                Product.Parameter.TargetType.ConstructorParameter,
                defaultValues.get(name).map(value => c.Expr[tpe.Underlying](value))
              )
            }
        }))

        val setters =
          A.decls.sorted
            .to(List)
            .filterNot(isGarbageSymbol)
            .collect { case m if m.isMethod => m.asMethod }
            .filter(isJavaSetterOrVar)
            .map { setter =>
              // Scala 3's JB setters _are_ methods ending with _= due to change in @BeanProperty behavior.
              // We have to drop that suffix to align names, so that comparing is possible.
              val n: String = getDecodedName(setter)
              val name =
                if (isVar(setter)) n.stripSuffix("_$eq").stripSuffix("_=") else n
              name -> setter
            }
            .filter { case (name, _) => !paramTypes.keySet.contains(name) } // _exact_ name match!
            .map { case (name, setter) =>
              val termName = setter.asTerm.name.toTermName
              val tpe = ExistentialType(fromUntyped(paramListsOf(Type[A].tpe, setter).flatten.head.typeSignature))
              (
                name,
                termName,
                tpe.mapK[Product.Parameter](_ =>
                  _ =>
                    Product.Parameter(
                      targetType = Product.Parameter.TargetType
                        .SetterParameter(ExistentialType(fromUntyped(returnTypeOf(Type[A].tpe, setter)))),
                      defaultValue = None
                    )
                )
              )
            }
        val setterParameters = ListMap.from(setters.map { case (name, _, param) => name -> param })
        type Setter[B] = (Expr[A], Expr[B]) => Expr[Unit]
        val setterExprs = setters.map { case (name, termName, param) =>
          name ->
            param.mapK[Setter] {
              implicit Param: Type[param.Underlying] => setter => (exprA: Expr[A], exprArg: Expr[param.Underlying]) =>
                c.Expr[Unit](q"$exprA.$termName($exprArg)")
            }
        }.toMap

        val parameters: Product.Parameters = constructorParameters ++ setterParameters

        val constructor: Product.Arguments => Expr[A] = arguments => {
          val (constructorArguments, setterArguments) = checkArguments[A](parameters, arguments)

          def newExpr =
            c.Expr[A](q"new $A(...${paramss.map(_.map(param => constructorArguments(paramNames(param)).value))})")

          if (setterArguments.isEmpty) {
            newExpr
          } else {
            PrependDefinitionsTo
              .prependVal[A](newExpr, ExprPromise.NameGenerationStrategy.FromType)
              .use { exprA =>
                Expr.block(
                  setterArguments.map { case (name, exprArg) =>
                    val setter = setterExprs(name)
                    assert(exprArg.Underlying =:= setter.Underlying)
                    import setter.value as setterExpr
                    setterExpr(exprA, exprArg.value.asInstanceOf[Expr[setter.Underlying]])
                  }.toList,
                  exprA
                )
              }
          }
        }

        Some(Product.Constructor(parameters, constructor))
      } else None
    }

    def exprAsInstanceOfMethod[A: Type](args: List[ListMap[String, ??]])(expr: Expr[Any]): Product.Constructor[A] = {
      val parameters: Product.Parameters = ListMap.from(for {
        list <- args
        pair <- list.toList
        (paramName, paramType) = pair
      } yield {
        import paramType.Underlying as ParamType
        paramName -> Existential[Product.Parameter, ParamType](
          Product.Parameter(Product.Parameter.TargetType.ConstructorParameter, None)
        )
      })

      val constructor: Product.Arguments => Expr[A] = arguments => {
        val (constructorArguments, _) = checkArguments[A](parameters, arguments)

        val methodType: ?? = args.foldRight[??](Type[A].as_??) { (paramList, resultType) =>
          val paramTypes = paramList.values.map(_.Underlying.tpe).toList
          // tq returns c.Tree, to turn it to c.Type we need .tpe, which without a .typecheck is null
          fromUntyped(c.typecheck(tq"(..$paramTypes) => ${resultType.Underlying.tpe}", mode = c.TYPEmode).tpe).as_??
        }

        import methodType.Underlying as MethodType
        val tree = expr.asInstanceOfExpr[MethodType].tree
        c.Expr[A](q"$tree(...${(args.map(_.map { case (paramName, _) =>
            constructorArguments(paramName).value.tree
          }))})")
      }

      Product.Constructor[A](parameters, constructor)
    }

    private val getDecodedName = (s: Symbol) => s.name.decodedName.toString

    private val isGarbageSymbol = getDecodedName andThen ProductTypes.isGarbageName

    // Borrowed from jsoniter-scala: https://github.com/plokhotnyuk/jsoniter-scala/blob/b14dbe51d3ae6752e5a9f90f1f3caf5bceb5e4b0/jsoniter-scala-macros/shared/src/main/scala/com/github/plokhotnyuk/jsoniter_scala/macros/JsonCodecMaker.scala#L462
    private def companionSymbol[A: Type]: Symbol = {
      val sym = Type[A].tpe.typeSymbol
      val comp = sym.companion
      if (comp.isModule) comp
      else {
        val ownerChainOf: Symbol => Iterator[Symbol] =
          s => Iterator.iterate(s)(_.owner).takeWhile(x => x != null && x != NoSymbol).toVector.reverseIterator
        val path = ownerChainOf(sym)
          .zipAll(ownerChainOf(c.internal.enclosingOwner), NoSymbol, NoSymbol)
          .dropWhile { case (x, y) => x == y }
          .takeWhile(_._1 != NoSymbol)
          .map(_._1.name.toTermName)
        // $COVERAGE-OFF$should never happen unless someone mess around with type-level representation
        if (path.isEmpty) assertionFailed(s"Cannot find a companion for ${Type.prettyPrint[A]}")
        else c.typecheck(path.foldLeft[Tree](Ident(path.next()))(Select(_, _)), silent = true).symbol
        // $COVERAGE-ON$
      }
    }
  }
}
