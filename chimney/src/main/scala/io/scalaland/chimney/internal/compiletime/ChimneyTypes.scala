package io.scalaland.chimney.internal.compiletime

import io.scalaland.chimney.*
import io.scalaland.chimney.dsl.TransformerDefinitionCommons.RuntimeDataStore
import io.scalaland.chimney.dsl.{ImplicitTransformerPreference, TransformerDefinitionCommons}

private[compiletime] trait ChimneyTypes { this: Types with Existentials =>

  protected val ChimneyType: ChimneyTypeModule
  protected trait ChimneyTypeModule {

    def Transformer[From: Type, To: Type]: Type[Transformer[From, To]]
    def PartialTransformer[From: Type, To: Type]: Type[PartialTransformer[From, To]]
    def Patcher[A: Type, Patch: Type]: Type[Patcher[A, Patch]]

    val PartialResult: PartialResultModule
    trait PartialResultModule { this: PartialResult.type =>
      def apply[A: Type]: Type[partial.Result[A]]
      def unapply[A](tpe: Type[A]): Option[ExistentialType]

      def Value[A: Type]: Type[partial.Result.Value[A]]
      val Errors: Type[partial.Result.Errors]
    }

    val PathElement: PathElementModule
    trait PathElementModule { this: PathElement.type =>
      val tpe: Type[partial.PathElement]
      val Accessor: Type[partial.PathElement.Accessor]
      val Index: Type[partial.PathElement.Index]
      val MapKey: Type[partial.PathElement.MapKey]
      val MapValue: Type[partial.PathElement.MapValue]
    }

    val PreferTotalTransformer: Type[io.scalaland.chimney.dsl.PreferTotalTransformer.type]
    val PreferPartialTransformer: Type[io.scalaland.chimney.dsl.PreferPartialTransformer.type]

    val RuntimeDataStore: Type[TransformerDefinitionCommons.RuntimeDataStore]

    val TransformerCfg: TransformerCfgModule
    trait TransformerCfgModule {
      val Empty: Type[internal.TransformerCfg.Empty]
      val FieldConst: FieldConstModule
      trait FieldConstModule
          extends Constructor2Bounded[
            Nothing,
            String,
            Nothing,
            internal.TransformerCfg,
            internal.TransformerCfg.FieldConst
          ] { this: FieldConst.type => }
      val FieldConstPartial: FieldConstPartialModule
      trait FieldConstPartialModule
          extends Constructor2Bounded[
            Nothing,
            String,
            Nothing,
            internal.TransformerCfg,
            internal.TransformerCfg.FieldConstPartial
          ] { this: FieldConstPartial.type => }
      val FieldComputed: FieldComputedModule
      trait FieldComputedModule
          extends Constructor2Bounded[
            Nothing,
            String,
            Nothing,
            internal.TransformerCfg,
            internal.TransformerCfg.FieldComputed
          ] { this: FieldComputed.type => }
      val FieldComputedPartial: FieldComputedPartialModule
      trait FieldComputedPartialModule
          extends Constructor2Bounded[
            Nothing,
            String,
            Nothing,
            internal.TransformerCfg,
            internal.TransformerCfg.FieldComputedPartial
          ] { this: FieldComputedPartial.type => }
      val FieldRelabelled: FieldRelabelledModule
      trait FieldRelabelledModule
          extends Constructor3Bounded[
            Nothing,
            String,
            Nothing,
            String,
            Nothing,
            internal.TransformerCfg,
            internal.TransformerCfg.FieldRelabelled
          ] { this: FieldRelabelled.type => }
      val CoproductInstance: CoproductInstanceModule
      trait CoproductInstanceModule
          extends Constructor3Bounded[
            Nothing,
            Any,
            Nothing,
            Any,
            Nothing,
            internal.TransformerCfg,
            internal.TransformerCfg.CoproductInstance
          ] { this: CoproductInstance.type => }
      val CoproductInstancePartial: CoproductInstancePartialModule
      trait CoproductInstancePartialModule
          extends Constructor3Bounded[
            Nothing,
            Any,
            Nothing,
            Any,
            Nothing,
            internal.TransformerCfg,
            internal.TransformerCfg.CoproductInstancePartial
          ] { this: CoproductInstancePartial.type => }
    }

    val TransformerFlags: TransformerFlagsModule
    trait TransformerFlagsModule { this: TransformerFlags.type =>
      val Default: Type[internal.TransformerFlags.Default]
      val Enable: EnableModule
      trait EnableModule
          extends Constructor2Bounded[
            Nothing,
            internal.TransformerFlags.Flag,
            Nothing,
            internal.TransformerFlags,
            internal.TransformerFlags.Enable
          ] { this: Enable.type => }
      val Disable: DisableModule
      trait DisableModule
          extends Constructor2Bounded[
            Nothing,
            internal.TransformerFlags.Flag,
            Nothing,
            internal.TransformerFlags,
            internal.TransformerFlags.Disable
          ] { this: Disable.type => }

      val Flags: FlagsModule
      trait FlagsModule { this: Flags.type =>
        val DefaultValues: Type[internal.TransformerFlags.DefaultValues]
        val BeanGetters: Type[internal.TransformerFlags.BeanGetters]
        val BeanSetters: Type[internal.TransformerFlags.BeanSetters]
        val MethodAccessors: Type[internal.TransformerFlags.MethodAccessors]
        val OptionDefaultsToNone: Type[internal.TransformerFlags.OptionDefaultsToNone]
        val ImplicitConflictResolution: ImplicitConflictResolutionModule
        trait ImplicitConflictResolutionModule
            extends Constructor1Bounded[
              Nothing,
              ImplicitTransformerPreference,
              internal.TransformerFlags.ImplicitConflictResolution
            ] { this: ImplicitConflictResolution.type => }
        val MacrosLogging: Type[internal.TransformerFlags.MacrosLogging]
      }
    }
  }

  // you can import TypeImplicits.* in your shared code to avoid providing types manually, while avoiding conflicts with
  // implicit types seen in platform-specific scopes
  protected object ChimneyTypeImplicits {

    implicit def TransformerType[From: Type, To: Type]: Type[Transformer[From, To]] = ChimneyType.Transformer[From, To]
    implicit def PartialTransformerType[From: Type, To: Type]: Type[PartialTransformer[From, To]] =
      ChimneyType.PartialTransformer[From, To]
    implicit def PatcherType[A: Type, Patch: Type]: Type[Patcher[A, Patch]] = ChimneyType.Patcher[A, Patch]

    implicit def PartialResultType[A: Type]: Type[partial.Result[A]] = ChimneyType.PartialResult[A]
    implicit def PartialResultValueType[A: Type]: Type[partial.Result.Value[A]] = ChimneyType.PartialResult.Value[A]
    implicit val PartialResultErrorsType: Type[partial.Result.Errors] = ChimneyType.PartialResult.Errors

    implicit val PathElementType: Type[partial.PathElement] = ChimneyType.PathElement.tpe
    implicit val PathElementAccessor: Type[partial.PathElement.Accessor] = ChimneyType.PathElement.Accessor
    implicit val PathElementIndex: Type[partial.PathElement.Index] = ChimneyType.PathElement.Index
    implicit val PathElementMapKey: Type[partial.PathElement.MapKey] = ChimneyType.PathElement.MapKey
    implicit val PathElementMapValue: Type[partial.PathElement.MapValue] = ChimneyType.PathElement.MapValue

    implicit val RuntimeDataStoreType: Type[RuntimeDataStore] = ChimneyType.RuntimeDataStore
  }
}
