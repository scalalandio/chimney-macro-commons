package io.scalaland.chimney.dsl

import io.scalaland.chimney.internal.compiletime.derivation.patcher.PatcherMacros
import io.scalaland.chimney.internal.runtime.{PatcherFlags, PatcherOverrides, WithRuntimeDataStore}

import scala.language.experimental.macros

/** Provides operations to customize [[io.scalaland.chimney.Patcher]] logic for specific object value and patch value.
  *
  * @tparam A
  *   type of object to apply patch to
  * @tparam Patch
  *   type of patch object
  * @tparam Overrides
  *   type-level encoded config
  * @tparam Flags
  *   type-level encoded flags
  * @param obj
  *   object to patch
  * @param objPatch
  *   patch object
  * @param pd
  *   patcher definition
  *
  * @since 0.4.0
  */
final class PatcherUsing[A, Patch, Overrides <: PatcherOverrides, Flags <: PatcherFlags](
    val obj: A,
    val objPatch: Patch,
    val pd: PatcherDefinition[A, Patch, Overrides, Flags]
) extends PatcherFlagsDsl[Lambda[`Flags1 <: PatcherFlags` => PatcherUsing[A, Patch, Overrides, Flags1]], Flags]
    with WithRuntimeDataStore {

  /** Applies configured patching in-place.
    *
    * @return
    *   patched value
    *
    * @since 0.4.0
    */
  def patch[ImplicitScopeFlags <: PatcherFlags](implicit
      pc: PatcherConfiguration[ImplicitScopeFlags]
  ): A = macro PatcherMacros.derivePatchWithConfig[A, Patch, Overrides, Flags, ImplicitScopeFlags]

  // FIXME: (2.0.0 cleanup) - kept to make MiMa happy
  // $COVERAGE-OFF$
  def this(obj: A, objPatch: Patch) = this(obj, objPatch, new PatcherDefinition())
  // $COVERAGE-ON$

  private[chimney] def addOverride(overrideData: Any): this.type =
    new PatcherUsing(obj, objPatch, pd.addOverride(overrideData)).asInstanceOf[this.type]
}
