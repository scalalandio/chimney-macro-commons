package io.scalaland.chimney.benchmarks

import org.openjdk.jmh.annotations.Benchmark
import io.scalaland.chimney.dsl._
import io.scalaland.chimney.Transformer

class DslBenchmark extends CommonBenchmarkSettings {
  import fixtures._

  private val longRenameT: Transformer[Long, LongRenamedOutput] =
    Transformer.define[Long, LongRenamedOutput]
      .withFieldRenamed(_.a, _.a$)
      .withFieldRenamed(_.b, _.b$)
      .withFieldRenamed(_.c, _.c$)
      .withFieldRenamed(_.d, _.d$)
      .withFieldRenamed(_.e, _.e$)
      .withFieldRenamed(_.f, _.f$)
      .withFieldRenamed(_.g, _.g$)
      .withFieldRenamed(_.h, _.h$)
      .withFieldRenamed(_.i, _.i$)
      .withFieldRenamed(_.j, _.j$)
      .withFieldRenamed(_.k, _.k$)
      .withFieldRenamed(_.l, _.l$)
      .withFieldRenamed(_.m, _.m$)
      .withFieldRenamed(_.n, _.n$)
      .withFieldRenamed(_.o, _.o$)
      .withFieldRenamed(_.p, _.p$)
      .withFieldRenamed(_.q, _.q$)
      .withFieldRenamed(_.r, _.r$)
      .withFieldRenamed(_.s, _.s$)
      .withFieldRenamed(_.t, _.t$)
      .withFieldRenamed(_.u, _.u$)
      .withFieldRenamed(_.v, _.v$)
      .buildTransformer

  private val longComputeT: Transformer[Long, LongOutput] =
    Transformer.define[Long, LongOutput]
      .withFieldComputed(_.a, _.a * 2)
      .withFieldComputed(_.b, _.b * 2)
      .withFieldComputed(_.c, _.c * 2)
      .withFieldComputed(_.d, _.d * 2)
      .withFieldComputed(_.e, _.e * 2)
      .withFieldComputed(_.f, _.f * 2)
      .withFieldComputed(_.g, _.g * 2)
      .withFieldComputed(_.h, _.h * 2)
      .withFieldComputed(_.i, _.i * 2)
      .withFieldComputed(_.j, _.j * 2)
      .withFieldComputed(_.k, _.k * 2)
      .withFieldComputed(_.l, _.l * 2)
      .withFieldComputed(_.m, _.m * 2)
      .withFieldComputed(_.n, _.n * 2)
      .withFieldComputed(_.o, _.o * 2)
      .withFieldComputed(_.p, _.p * 2)
      .withFieldComputed(_.q, _.q * 2)
      .withFieldComputed(_.r, _.r * 2)
      .withFieldComputed(_.s, _.s * 2)
      .withFieldComputed(_.t, _.t * 2)
      .withFieldComputed(_.u, _.u * 2)
      .withFieldComputed(_.v, _.v * 2)
      .buildTransformer

  private val longConstT: Transformer[Long, LongOutput] =
    Transformer.define[Long, LongOutput]
      .withFieldConst(_.a, 834)
      .withFieldConst(_.b, 834)
      .withFieldConst(_.c, 834)
      .withFieldConst(_.d, 834)
      .withFieldConst(_.e, 834)
      .withFieldConst(_.f, 834)
      .withFieldConst(_.g, 834)
      .withFieldConst(_.h, 834)
      .withFieldConst(_.i, 834)
      .withFieldConst(_.j, 834)
      .withFieldConst(_.k, 834)
      .withFieldConst(_.l, 834)
      .withFieldConst(_.m, 834)
      .withFieldConst(_.n, 834)
      .withFieldConst(_.o, 834)
      .withFieldConst(_.p, 834)
      .withFieldConst(_.q, 834)
      .withFieldConst(_.r, 834)
      .withFieldConst(_.s, 834)
      .withFieldConst(_.t, 834)
      .withFieldConst(_.u, 834)
      .withFieldConst(_.v, 834)
      .buildTransformer

  var longSample: Long = samples.longSample

  @Benchmark
  def longWithRenamesChimneyWithDsl: LongRenamedOutput =
    longSample.into[LongRenamedOutput]
      .withFieldRenamed(_.a, _.a$)
      .withFieldRenamed(_.b, _.b$)
      .withFieldRenamed(_.c, _.c$)
      .withFieldRenamed(_.d, _.d$)
      .withFieldRenamed(_.e, _.e$)
      .withFieldRenamed(_.f, _.f$)
      .withFieldRenamed(_.g, _.g$)
      .withFieldRenamed(_.h, _.h$)
      .withFieldRenamed(_.i, _.i$)
      .withFieldRenamed(_.j, _.j$)
      .withFieldRenamed(_.k, _.k$)
      .withFieldRenamed(_.l, _.l$)
      .withFieldRenamed(_.m, _.m$)
      .withFieldRenamed(_.n, _.n$)
      .withFieldRenamed(_.o, _.o$)
      .withFieldRenamed(_.p, _.p$)
      .withFieldRenamed(_.q, _.q$)
      .withFieldRenamed(_.r, _.r$)
      .withFieldRenamed(_.s, _.s$)
      .withFieldRenamed(_.t, _.t$)
      .withFieldRenamed(_.u, _.u$)
      .withFieldRenamed(_.v, _.v$)
      .transform

  @Benchmark
  def longWithRenamesChimney: LongRenamedOutput = longRenameT.transform(longSample)

  @Benchmark
  def longWithRenamesByHand: LongRenamedOutput = doLongRenameByHand(longSample)

  @Benchmark
  def longWithComputeChimneyWithDsl: LongOutput =
    longSample.into[LongOutput]
      .withFieldComputed(_.a, _.a * 2)
      .withFieldComputed(_.b, _.b * 2)
      .withFieldComputed(_.c, _.c * 2)
      .withFieldComputed(_.d, _.d * 2)
      .withFieldComputed(_.e, _.e * 2)
      .withFieldComputed(_.f, _.f * 2)
      .withFieldComputed(_.g, _.g * 2)
      .withFieldComputed(_.h, _.h * 2)
      .withFieldComputed(_.i, _.i * 2)
      .withFieldComputed(_.j, _.j * 2)
      .withFieldComputed(_.k, _.k * 2)
      .withFieldComputed(_.l, _.l * 2)
      .withFieldComputed(_.m, _.m * 2)
      .withFieldComputed(_.n, _.n * 2)
      .withFieldComputed(_.o, _.o * 2)
      .withFieldComputed(_.p, _.p * 2)
      .withFieldComputed(_.q, _.q * 2)
      .withFieldComputed(_.r, _.r * 2)
      .withFieldComputed(_.s, _.s * 2)
      .withFieldComputed(_.t, _.t * 2)
      .withFieldComputed(_.u, _.u * 2)
      .withFieldComputed(_.v, _.v * 2)
      .transform

  @Benchmark
  def longWithComputeChimney: LongOutput = longComputeT.transform(longSample)

  @Benchmark
  def longWithComputeByHand: LongOutput = doLongByHandComputed(longSample)

  @Benchmark
  def longWithConstChimneyWithDsl: LongOutput =
    longSample.into[LongOutput]
      .withFieldConst(_.a, 834)
      .withFieldConst(_.b, 834)
      .withFieldConst(_.c, 834)
      .withFieldConst(_.d, 834)
      .withFieldConst(_.e, 834)
      .withFieldConst(_.f, 834)
      .withFieldConst(_.g, 834)
      .withFieldConst(_.h, 834)
      .withFieldConst(_.i, 834)
      .withFieldConst(_.j, 834)
      .withFieldConst(_.k, 834)
      .withFieldConst(_.l, 834)
      .withFieldConst(_.m, 834)
      .withFieldConst(_.n, 834)
      .withFieldConst(_.o, 834)
      .withFieldConst(_.p, 834)
      .withFieldConst(_.q, 834)
      .withFieldConst(_.r, 834)
      .withFieldConst(_.s, 834)
      .withFieldConst(_.t, 834)
      .withFieldConst(_.u, 834)
      .withFieldConst(_.v, 834)
      .transform

  @Benchmark
  def longWithConstChimney: LongOutput = longConstT.transform(longSample)

  @Benchmark
  def longWithConstByHand: LongOutput = doLongByHandConst(longSample)

}
