package io.scalaland.chimney.internal.compiletime.derivation.iso

import io.scalaland.chimney.dsl
import io.scalaland.chimney.Iso
import io.scalaland.chimney.internal.compiletime.derivation.transformer.{DerivationPlatform, Gateway}
import io.scalaland.chimney.internal.runtime

import scala.reflect.macros.blackbox

final class IsoMacros(val c: blackbox.Context) extends DerivationPlatform with Gateway {

  import c.universe.{internal as _, Transformer as _, *}

  def deriveIsoWithDefaults[
      From: WeakTypeTag,
      To: WeakTypeTag
  ]: c.universe.Expr[Iso[From, To]] =
    retypecheck(
      suppressWarnings(
        resolveImplicitScopeConfigAndMuteUnusedWarnings { implicitScopeFlagsType =>
          import implicitScopeFlagsType.Underlying
          c.Expr(
            q"""
            io.scalaland.chimney.Iso[${Type[From]}, ${Type[To]}](
              from = ${deriveTotalTransformer[
                From,
                To,
                runtime.TransformerOverrides.Empty,
                runtime.TransformerFlags.Default,
                implicitScopeFlagsType.Underlying
              ](ChimneyExpr.RuntimeDataStore.empty)},
              to = ${derivePartialTransformer[
                To,
                From,
                runtime.TransformerOverrides.Empty,
                runtime.TransformerFlags.Default,
                implicitScopeFlagsType.Underlying
              ](ChimneyExpr.RuntimeDataStore.empty)}
            )
            """
          )
        }
      )
    )

  def deriveIsoWithConfig[
      From: WeakTypeTag,
      To: WeakTypeTag,
      FromOverrides <: runtime.TransformerOverrides: WeakTypeTag,
      ToOverrides <: runtime.TransformerOverrides: WeakTypeTag,
      InstanceFlags <: runtime.TransformerFlags: WeakTypeTag,
      ImplicitScopeFlags <: runtime.TransformerFlags: WeakTypeTag
  ](
      tc: Expr[io.scalaland.chimney.dsl.TransformerConfiguration[ImplicitScopeFlags]]
  ): Expr[Iso[From, To]] = retypecheck(
    suppressWarnings(
      Expr.block(
        List(Expr.suppressUnused(tc)),
        c.Expr(
          q"""
          io.scalaland.chimney.Iso[${Type[From]}, ${Type[To]}](
            from = ${deriveTotalTransformer[
              From,
              To,
              FromOverrides,
              InstanceFlags,
              ImplicitScopeFlags
            ](
              // Called by CodecDefinition => prefix is CodecDefinition
              c.Expr[dsl.TransformerDefinitionCommons.RuntimeDataStore](q"${c.prefix.tree}.from.runtimeData")
            )},
            to = ${deriveTotalTransformer[
              To,
              From,
              ToOverrides,
              InstanceFlags,
              ImplicitScopeFlags
            ](
              // Called by CodecDefinition => prefix is CodecDefinition
              c.Expr[dsl.TransformerDefinitionCommons.RuntimeDataStore](q"${c.prefix.tree}.to.runtimeData")
            )}
          )
          """
        )
      )
    )
  )

  private def resolveImplicitScopeConfigAndMuteUnusedWarnings[A: Type](
      useImplicitScopeFlags: ?<[runtime.TransformerFlags] => Expr[A]
  ): Expr[A] = {
    val implicitScopeConfig = {
      val transformerConfigurationType = Type.platformSpecific
        .fromUntyped[io.scalaland.chimney.dsl.TransformerConfiguration[? <: runtime.TransformerFlags]](
          c.typecheck(
            tree = tq"${typeOf[io.scalaland.chimney.dsl.TransformerConfiguration[? <: runtime.TransformerFlags]]}",
            silent = true,
            mode = c.TYPEmode,
            withImplicitViewsDisabled = true,
            withMacrosDisabled = false
          ).tpe
        )

      Expr.summonImplicit(transformerConfigurationType).getOrElse {
        // $COVERAGE-OFF$
        reportError("Can't locate implicit TransformerConfiguration!")
        // $COVERAGE-ON$
      }
    }
    val implicitScopeFlagsType = Type.platformSpecific
      .fromUntyped[runtime.TransformerFlags](implicitScopeConfig.tpe.tpe.typeArgs.head)
      .as_?<[runtime.TransformerFlags]

    Expr.block(
      List(Expr.suppressUnused(implicitScopeConfig)),
      useImplicitScopeFlags(implicitScopeFlagsType)
    )
  }

  private def retypecheck[A: Type](expr: c.Expr[A]): c.Expr[A] = try
    c.Expr[A](c.typecheck(tree = c.untypecheck(expr.tree)))
  catch {
    case scala.reflect.macros.TypecheckException(_, msg) => c.abort(c.enclosingPosition, msg)
  }

  private def suppressWarnings[A: Type](expr: c.Expr[A]): c.Expr[A] = {
    // Scala 3 generate prefix$macro$[n] while Scala 2 prefix[n] and we want to align the behavior
    val result = c.internal.reificationSupport.freshTermName("result$macro$")
    c.Expr[A](
      q"""{
          @_root_.java.lang.SuppressWarnings(_root_.scala.Array("org.wartremover.warts.All", "all"))
          val $result = $expr
          $result
        }"""
    )
  }
}
