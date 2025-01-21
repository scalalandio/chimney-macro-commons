package io.scalaland.chimney.dsl

import io.scalaland.chimney.{partial, PartialTransformer}
import io.scalaland.chimney.internal.compiletime.derivation.transformer.TransformerMacros
import io.scalaland.chimney.internal.compiletime.dsl.PartialTransformerDefinitionMacros
import io.scalaland.chimney.internal.runtime.{
  IsFunction,
  Path,
  TransformerFlags,
  TransformerOverrides,
  WithRuntimeDataStore
}

import scala.language.experimental.macros

/** Allows customization of [[io.scalaland.chimney.PartialTransformer]] derivation.
  *
  * @tparam From
  *   type of input value
  * @tparam To
  *   type of output value
  * @tparam Overrides
  *   type-level encoded config
  * @tparam Flags
  *   type-level encoded flags
  *
  * @since 0.7.0
  */
final class PartialTransformerDefinition[From, To, Overrides <: TransformerOverrides, Flags <: TransformerFlags](
    val runtimeData: TransformerDefinitionCommons.RuntimeDataStore
) extends TransformerFlagsDsl[Lambda[
      `Flags1 <: TransformerFlags` => PartialTransformerDefinition[From, To, Overrides, Flags1]
    ], Flags]
    with TransformerDefinitionCommons[
      Lambda[`Overrides1 <: TransformerOverrides` => PartialTransformerDefinition[From, To, Overrides1, Flags]]
    ]
    with WithRuntimeDataStore {

  /** Use provided `value` for field picked using `selector`.
    *
    * By default, if `From` is missing field picked by `selector`, compilation fails.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#wiring-the-constructors-parameter-to-a-provided-value]]
    *   for more details
    *
    * @tparam T
    *   type of target field
    * @tparam U
    *   type of provided value
    * @param selector
    *   target field in `To`, defined like `_.name`
    * @param value
    *   constant value to use for the target field
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 0.7.0
    */
  def withFieldConst[T, U](selector: To => T, value: U)(implicit
      ev: U <:< T
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFieldConstImpl[From, To, Overrides, Flags]

  /** Use provided partial result `value` for field picked using `selector`.
    *
    * By default, if `From` is missing field picked by `selector`, compilation fails.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#wiring-the-constructors-parameter-to-a-provided-value]]
    *   for more details
    *
    * @tparam T
    *   type of target field
    * @tparam U
    *   type of computed value
    * @param selector
    *   target field in `To`, defined like `_.name`
    * @param value
    *   constant value to use for the target field
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 0.7.0
    */
  def withFieldConstPartial[T, U](selector: To => T, value: partial.Result[U])(implicit
      ev: U <:< T
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFieldConstPartialImpl[From, To, Overrides, Flags]

  /** Use the function `f` to compute a value of the field picked using the `selector`.
    *
    * By default, if `From` is missing a field and it's not provided with some `selector`, the compilation fails.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#wiring-the-constructors-parameter-to-computed-value]]
    *   for more details
    *
    * @tparam T
    *   type of target field
    * @tparam U
    *   type of computed value
    * @param selector
    *   target field in `To`, defined like `_.name`
    * @param f
    *   function used to compute value of the target field
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 0.7.0
    */
  def withFieldComputed[T, U](selector: To => T, f: From => U)(implicit
      ev: U <:< T
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFieldComputedImpl[From, To, Overrides, Flags]

  /** Use the function `f` to compute a value of the field picked using the `selectorTo` from a value extracted with
    * `selectorFrom` as an input.
    *
    * By default, if `From` is missing a field and it's not provided with some `selectorTo`, the compilation fails.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#wiring-the-constructors-parameter-to-computed-value]]
    *   for more details
    *
    * @tparam S
    *   * type of source field
    * @tparam T
    *   type of target field
    * @tparam U
    *   type of computed value
    * @param selectorFrom
    *   source field in `From`, defined like `_.name`
    * @param selectorTo
    *   target field in `To`, defined like `_.name`
    * @param f
    *   function used to compute value of the target field
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 1.6.0
    */
  def withFieldComputedFrom[S, T, U](selectorFrom: From => S)(selectorTo: To => T, f: S => U)(implicit
      ev: U <:< T
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFieldComputedFromImpl[From, To, Overrides, Flags]

  /** Use the function `f` to compute a partial result for the field picked using the `selector`.
    *
    * By default, if `From` is missing a field and it's not provided with some `selector`, the compilation fails.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#wiring-the-constructors-parameter-to-computed-value]]
    *   for more details
    *
    * @tparam T
    *   type of target field
    * @tparam U
    *   type of computed value
    * @param selector
    *   target field in `To`, defined like `_.name`
    * @param f
    *   function used to compute value of the target field
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 0.7.0
    */
  def withFieldComputedPartial[T, U](
      selector: To => T,
      f: From => partial.Result[U]
  )(implicit ev: U <:< T): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFieldComputedPartialImpl[From, To, Overrides, Flags]

  /** Use the function `f` to compute a partial result of the field picked using the `selectorTo` from a value extracted
    * with `selectorFrom` as an input.
    *
    * By default, if `From` is missing a field and it's not provided with some `selector`, the compilation fails.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#wiring-the-constructors-parameter-to-computed-value]]
    *   for more details
    *
    * @tparam S
    *   * type of source field
    * @tparam T
    *   type of target field
    * @tparam U
    *   type of computed value
    * @param selectorFrom
    *   source field in `From`, defined like `_.name`
    * @param selectorTo
    *   target field in `To`, defined like `_.name`
    * @param f
    *   function used to compute value of the target field
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 1.6.0
    */
  def withFieldComputedPartialFrom[S, T, U](selectorFrom: From => S)(
      selectorTo: To => T,
      f: S => partial.Result[U]
  )(implicit ev: U <:< T): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFieldComputedPartialFromImpl[From, To, Overrides, Flags]

  /** Use the `selectorFrom` field in `From` to obtain the value of the `selectorTo` field in `To`.
    *
    * By default, if `From` is missing a field and it's not provided with some `selectorTo`, the compilation fails.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#wiring-the-constructors-parameter-to-its-source-field]]
    *   for more details
    *
    * @tparam T
    *   type of source field
    * @tparam U
    *   type of target field
    * @param selectorFrom
    *   source field in `From`, defined like `_.originalName`
    * @param selectorTo
    *   target field in `To`, defined like `_.newName`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 0.7.0
    */
  def withFieldRenamed[T, U](
      selectorFrom: From => T,
      selectorTo: To => U
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFieldRenamedImpl[From, To, Overrides, Flags]

  /** Mark field as expected to be unused when it would fail [[UnusedFieldPolicy]] by default.
    *
    * @see
    *   TODO
    *
    * @tparam T
    *   type of source field
    * @param selectorFrom
    *   source field in `From`, defined like `_.originalName`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since TODO
    */
  def withFieldUnused[T](
      selectorFrom: From => T
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFieldUnusedImpl[From, To, Overrides, Flags]

  /** Use `f` to calculate the unmatched subtype when mapping one sealed/enum into another.
    *
    * By default, if mapping one coproduct in `From` into another coproduct in `To` derivation expects that coproducts
    * to have matching names of its components, and for every component in `To` field's type there is matching component
    * in `From` type. If some component is missing it fails compilation unless provided replacement with this operation.
    *
    * For convenience/readability [[withEnumCaseHandled]] alias can be used (e.g. for Scala 3 enums or Java enums).
    *
    * It differs from `withFieldComputed(_.matching[Subtype], src => ...)`, since `withSealedSubtypeHandled` matches on
    * a `From` subtype, while `.matching[Subtype]` matches on a `To` value's piece.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#handling-a-specific-sealed-subtype-with-a-computed-value]]
    *   for more details
    *
    * @tparam Subtype
    *   type of sealed/enum instance
    * @param f
    *   function to calculate values of components that cannot be mapped automatically
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 1.0.0
    */
  def withSealedSubtypeHandled[Subtype](
      f: Subtype => To
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withSealedSubtypeHandledImpl[From, To, Overrides, Flags, Subtype]

  /** Alias to [[withSealedSubtypeHandled]].
    *
    * @since 1.0.0
    */
  def withEnumCaseHandled[Subtype](
      f: Subtype => To
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withSealedSubtypeHandledImpl[From, To, Overrides, Flags, Subtype]

  /** Renamed to [[withSealedSubtypeHandled]].
    *
    * @since 0.7.0
    */
  @deprecated("Use .withSealedSubtypeHandled or .withEnumCaseHandled for more clarity", "1.0.0")
  def withCoproductInstance[Subtype](
      f: Subtype => To
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withSealedSubtypeHandledImpl[From, To, Overrides, Flags, Subtype]

  /** Use `f` to calculate the unmatched subtype's partial.Result when mapping one sealed/enum into another.
    *
    * By default, if mapping one coproduct in `From` into another coproduct in `To` derivation expects that coproducts
    * to have matching names of its components, and for every component in `To` field's type there is matching component
    * in `From` type. If some component is missing it fails compilation unless provided replacement with this operation.
    *
    * For convenience/readability [[withEnumCaseHandledPartial]] alias can be used (e.g. for Scala 3 enums or Java
    * enums).
    *
    * It differs from `withFieldComputedPartial(_.matching[Subtype], src => ...)`, since `withSealedSubtypeHandled`
    * matches on a `From` subtype, while `.matching[Subtype]` matches on a `To` value's piece.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#handling-a-specific-sealed-subtype-with-a-computed-value]]
    *   for more details
    *
    * @tparam Subtype
    *   type of sealed/enum instance
    * @param f
    *   function to calculate values of components that cannot be mapped automatically
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 0.7.0
    */
  def withSealedSubtypeHandledPartial[Subtype](
      f: Subtype => partial.Result[To]
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withSealedSubtypeHandledPartialImpl[From, To, Overrides, Flags, Subtype]

  /** Alias to [[withSealedSubtypeHandledPartial]].
    *
    * @since 1.0.0
    */
  def withEnumCaseHandledPartial[Subtype](
      f: Subtype => partial.Result[To]
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withSealedSubtypeHandledPartialImpl[From, To, Overrides, Flags, Subtype]

  /** Renamed to [[withSealedSubtypeHandledPartial]].
    *
    * @since 0.7.0
    */
  @deprecated("Use .withSealedSubtypeHandledPartial or .withEnumCaseHandledPartial for more clarity", "1.0.0")
  def withCoproductInstancePartial[Subtype](
      f: Subtype => partial.Result[To]
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withSealedSubtypeHandledPartialImpl[From, To, Overrides, Flags, Subtype]

  /** Use the `FromSubtype` in `From` as a source for the `ToSubtype` in `To`.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#handling-a-specific-sealed-subtype-by-a-specific-target-subtype]]
    *   for more details
    *
    * @tparam DomainSubtype
    *   type of sealed/enum instance
    * @tparam DtoSubtype
    *   type of sealed/enum instance
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 1.2.0
    */
  def withSealedSubtypeRenamed[FromSubtype, ToSubtype]
      : PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros
      .withSealedSubtypeRenamedImpl[From, To, Overrides, Flags, FromSubtype, ToSubtype]

  /** Alias to [[withSealedSubtypeRenamed]].
    *
    * @since 1.2.0
    */
  def withEnumCaseRenamed[FromSubtype, ToSubtype]
      : PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros
      .withSealedSubtypeRenamedImpl[From, To, Overrides, Flags, FromSubtype, ToSubtype]

  /** Mark subtype as expected to be unmatched when it would fail [[UnmatchedSubtypePolicy]] by default.
    *
    * @see
    *   TODO
    *
    * @tparam T
    *   type of subtype
    * @param selectorTo
    *   target subtype in `To`, defined like `_.matching[Subtype]`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since TODO
    */
  def withSealedSubtypeUnmatched[T](
      selectorTo: To => T
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withSealedSubtypeUnmatchedImpl[From, To, Overrides, Flags]

  /** Alias to [[withSealedSubtypeUnmatched]].
    *
    * @since TODO
    */
  def withEnumCaseUnmatched[T](
      selectorTo: To => T
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withSealedSubtypeUnmatchedImpl[From, To, Overrides, Flags]

  /** To use `fallback` when the source of type `From` is missing fields.
    *
    * Fallbacks can be stacked - then they will be tried in the order in which they were added.
    *
    * @see
    *   TODO
    *
    * @tparam FromFallback
    *   type of the fallback value which would be checked for fields when the `From` value would be missing
    * @param fallback
    *   fallback value which would be checked for fields when the `From` value would be missing
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since TODO
    */
  def withFallback[FromFallback](
      fallback: FromFallback
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFallbackImpl[From, To, Overrides, Flags, FromFallback]

  /** To use `fallback` when the source of type `T`, extracted with `selectorFrom`, is missing fields.
    *
    * Fallbacks can be stacked - then they will be tried in the order in which they were added.
    *
    * @see
    *   TODO
    *
    * @tparam T
    *   type of the source value that fallback is provided for
    * @tparam FromFallback
    *   type of the fallback value which would be checked for fields when the `From` value would be missing
    * @param selectorFrom
    *   path to the source value the fallback will be provided for
    * @param fallback
    *   fallback value which would be checked for fields when the `From` value would be missing
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since TODO
    */
  def withFallbackFrom[T, FromFallback](selectorFrom: From => T)(
      fallback: FromFallback
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withFallbackFromImpl[From, To, Overrides, Flags, FromFallback]

  /** Use `f` instead of the primary constructor to construct the `To` value.
    *
    * Macro will read the names of Eta-expanded method's/lambda's parameters and try to match them with `From` getters.
    *
    * Values for each parameter can be provided the same way as if they were normal constructor's arguments.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#types-with-manually-provided-constructors]] for more
    *   details
    *
    * @tparam Ctor
    *   type of the Eta-expanded method/lambda which should return `To`
    * @param f
    *   method name or lambda which constructs `To`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 0.8.4
    */
  def withConstructor[Ctor](
      f: Ctor
  )(implicit ev: IsFunction.Of[Ctor, To]): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withConstructorImpl[From, To, Overrides, Flags]

  /** Use `f` instead of the primary constructor to construct the value extracted from `To` using the `selector`.
    *
    * Macro will read the names of Eta-expanded method's/lambda's parameters and try to match them with `From` getters.
    *
    * Values for each parameter can be provided the same way as if they were normal constructor's arguments.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#types-with-manually-provided-constructors]] for more
    *   details
    *
    * @tparam T
    *   type of the value which would be constructed with a custom constructor
    * @tparam Ctor
    *   type of the Eta-expanded method/lambda which should return `T`
    * @param selector
    *   target field in `To`, defined like `_.name`
    * @param f
    *   method name or lambda which constructs `To`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 1.6.0
    */
  def withConstructorTo[T, Ctor](selector: To => T)(f: Ctor)(implicit
      ev: IsFunction.Of[Ctor, T]
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withConstructorToImpl[From, To, Overrides, Flags]

  /** Use `f` instead of the primary constructor to parse into `partial.Result[To]` value.
    *
    * Macro will read the names of Eta-expanded method's/lambda's parameters and try to match them with `From` getters.
    *
    * Values for each parameter can be provided the same way as if they were normal constructor's arguments.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#types-with-manually-provided-constructors]] for more
    *   details
    *
    * @tparam Ctor
    *   type of the Eta-expanded method/lambda which should return `partial.Result[To]`
    * @param f
    *   method name or lambda which constructs `partial.Result[To]`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 0.8.4
    */
  def withConstructorPartial[Ctor](
      f: Ctor
  )(implicit
      ev: IsFunction.Of[Ctor, partial.Result[To]]
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withConstructorPartialImpl[From, To, Overrides, Flags]

  /** Use `f` instead of the primary constructor to parse into the value extracted from `To` using the `selector`.
    *
    * Macro will read the names of Eta-expanded method's/lambda's parameters and try to match them with `From` getters.
    *
    * Values for each parameter can be provided the same way as if they were normal constructor's arguments.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#types-with-manually-provided-constructors]] for more
    *   details
    *
    * @tparam T
    *   type of the value which would be constructed with a custom constructor
    * @tparam Ctor
    *   type of the Eta-expanded method/lambda which should return `partial.Result[T]`
    * @param selector
    *   target field in `To`, defined like `_.name`
    * @param f
    *   method name or lambda which constructs `partial.Result[T]`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerInto]]
    *
    * @since 1.6.0
    */
  def withConstructorPartialTo[T, Ctor](selector: To => T)(
      f: Ctor
  )(implicit
      ev: IsFunction.Of[Ctor, partial.Result[T]]
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withConstructorPartialToImpl[From, To, Overrides, Flags]

  /** Use `f` instead of the primary constructor to parse into `Either[String, To]` value.
    *
    * Macro will read the names of Eta-expanded method's/lambda's parameters and try to match them with `From` getters.
    *
    * Values for each parameter can be provided the same way as if they were normal constructor's arguments.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#types-with-manually-provided-constructors]] for more
    *   details
    *
    * @tparam Ctor
    *   type of the Eta-expanded method/lambda which should return `Either[String, To]`
    * @param f
    *   method name or lambda which constructs `Either[String, To]`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 1.0.0
    */
  def withConstructorEither[Ctor](
      f: Ctor
  )(implicit
      ev: IsFunction.Of[Ctor, Either[String, To]]
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withConstructorEitherImpl[From, To, Overrides, Flags]

  /** Use `f` instead of the primary constructor to parse into the `Either` value extracted from `To` using the
    * `selector`.
    *
    * Macro will read the names of Eta-expanded method's/lambda's parameters and try to match them with `From` getters.
    *
    * Values for each parameter can be provided the same way as if they were normal constructor's arguments.
    *
    * @see
    *   [[https://chimney.readthedocs.io/supported-transformations/#types-with-manually-provided-constructors]] for more
    *   details
    *
    * @tparam T
    *   type of the value which would be constructed with a custom constructor
    * @tparam Ctor
    *   type of the Eta-expanded method/lambda which should return `Either[String, T]`
    * @param selector
    *   target field in `To`, defined like `_.name`
    * @param f
    *   method name or lambda which constructs `Either[String, T]`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 1.6.0
    */
  def withConstructorEitherTo[T, Ctor](selector: To => T)(
      f: Ctor
  )(implicit
      ev: IsFunction.Of[Ctor, Either[String, T]]
  ): PartialTransformerDefinition[From, To, ? <: TransformerOverrides, Flags] =
    macro PartialTransformerDefinitionMacros.withConstructorEitherToImpl[From, To, Overrides, Flags]

  /** Define a flag only on some source value using the `selectorFrom`, rather than globally.
    *
    * @see
    *   [[https://chimney.readthedocs.io/cookbook/#constraining-flags-to-a-specific-fieldsubtype]] for more details
    *
    * @tparam T
    *   type of the source field
    * @param selectorFrom
    *   source field in `From`, defined like `_.name`
    * @return
    *   [[io.scalaland.chimney.dsl.PartialTransformerDefinition]]
    *
    * @since 1.6.0
    */
  def withSourceFlag[T](
      selectorFrom: From => T
  ): TransformerSourceFlagsDsl.OfPartialTransformerDefinition[From, To, Overrides, Flags, ? <: Path] =
    macro PartialTransformerDefinitionMacros.withSourceFlagImpl[From, To, Overrides, Flags]

  /** Define a flag only on some source value using the `selectorTo`, rather than globally.
    *
    * @see
    *   [[https://chimney.readthedocs.io/cookbook/#constraining-flags-to-a-specific-fieldsubtype]] for more details
    *
    * @tparam T
    *   type of the target field
    * @param selectorTo
    *   target field in `To`, defined like `_.name`
    * @return
    *   [[io.scalaland.chimney.dsl.TransformerDefinition]]
    *
    * @since 1.6.0
    */
  def withTargetFlag[T](
      selectorTo: To => T
  ): TransformerTargetFlagsDsl.OfPartialTransformerDefinition[From, To, Overrides, Flags, ? <: Path] =
    macro PartialTransformerDefinitionMacros.withTargetFlagImpl[From, To, Overrides, Flags]

  /** Build Partial Transformer using current configuration.
    *
    * It runs macro that tries to derive instance of `PartialTransformer[From, To]`. When transformation can't be
    * derived, it results with compilation error.
    *
    * @return
    *   [[io.scalaland.chimney.PartialTransformer]] type class instance
    *
    * @since 0.7.0
    */
  def buildTransformer[ImplicitScopeFlags <: TransformerFlags](implicit
      tc: io.scalaland.chimney.dsl.TransformerConfiguration[ImplicitScopeFlags]
  ): PartialTransformer[From, To] =
    macro TransformerMacros.derivePartialTransformerWithConfig[From, To, Overrides, Flags, ImplicitScopeFlags]

  private[chimney] def addOverride(overrideData: Any): this.type =
    new PartialTransformerDefinition(overrideData +: runtimeData).asInstanceOf[this.type]
}
