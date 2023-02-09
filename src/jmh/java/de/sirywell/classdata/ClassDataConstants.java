package benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import static benchmark.ClassDataConstants.Divisor.forDivisor;

@Fork(value = 1)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 2)
@Warmup(iterations = 2)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ClassDataConstants {

  private static final byte[] classBytes;
  private static final RandomGeneratorFactory<RandomGenerator.LeapableGenerator> randomGeneratorFactory;

  static {
    randomGeneratorFactory = RandomGeneratorFactory.all().filter(RandomGeneratorFactory::isLeapable)
        .map(f -> (RandomGeneratorFactory<RandomGenerator.LeapableGenerator>) (RandomGeneratorFactory<?>) f)
        .max(Comparator.comparingInt(RandomGeneratorFactory::stateBits))
        .orElseThrow(UnsupportedOperationException::new);
    try {
      classBytes = Files.readAllBytes(Path.of("build/classes/java/jmh/benchmark", "ClassDataConstants$CalculateIndexBase.class"));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Param({"1", "100", "100000"})
  private static int size;
  @Param({"basic", "dynamic", "handcraft"})
  private static String variant;

  @State(Scope.Benchmark)
  public static class Data {
    private final int[] dividends;
    private final Division division;

    public Data() {
      var generator = randomGeneratorFactory.create(123);
      dividends = new int[size * 6];
      for (int i = 0; i < size * 6; i++) {
        dividends[i] = generator.nextInt();
      }
      int[] divisors = generator.ints(6, 1, 123).toArray();
      division = switch (variant) {
        case "basic" -> new Basic(
            divisors[0],
            divisors[1],
            divisors[2],
            divisors[3],
            divisors[4],
            divisors[5]
        );
        case "dynamic" -> createDynamic(divisors);
        case "handcraft" -> new Handcraft(
            forDivisor(divisors[0]),
            forDivisor(divisors[1]),
            forDivisor(divisors[2]),
            forDivisor(divisors[3]),
            forDivisor(divisors[4]),
            forDivisor(divisors[5])
        );
        default -> throw new IllegalStateException("Unexpected value: " + variant);
      };
    }
  }

  @Benchmark
  public void divide1(Data data, Blackhole bh) {
    Division division = data.division;
    int[] dividends = data.dividends;
    for (int i = 0; i < dividends.length; i += 6) {
      bh.consume(division.divide(dividends[i]));
    }
  }

  @Benchmark
  public void divide2(Data data, Blackhole bh) {
    Division division = data.division;
    int[] dividends = data.dividends;
    for (int i = 0; i < dividends.length; i += 6) {
      bh.consume(division.divide(dividends[i], dividends[i + 1]));
    }
  }

  @Benchmark
  public void divide3(Data data, Blackhole bh) {
    Division division = data.division;
    int[] dividends = data.dividends;
    for (int i = 0; i < dividends.length; i += 6) {
      bh.consume(division.divide(dividends[i], dividends[i + 1], dividends[3]));
    }
  }

  @Benchmark
  public void divide4(Data data, Blackhole bh) {
    Division division = data.division;
    int[] dividends = data.dividends;
    for (int i = 0; i < dividends.length; i += 6) {
      bh.consume(division.divide(dividends[i], dividends[i + 1], dividends[i + 2], dividends[i + 3]));
    }
  }

  @Benchmark
  public void divide5(Data data, Blackhole bh) {
    Division division = data.division;
    int[] dividends = data.dividends;
    for (int i = 0; i < dividends.length; i += 6) {
      bh.consume(division.divide(dividends[i], dividends[i + 1], dividends[i + 2], dividends[i + 3], dividends[i + 4]));
    }
  }

  @Benchmark
  public void divide6(Data data, Blackhole bh) {
    Division division = data.division;
    int[] dividends = data.dividends;
    for (int i = 0; i < dividends.length; i += 6) {
      bh.consume(division.divide(dividends[i], dividends[i + 1], dividends[i + 2], dividends[i + 3], dividends[i + 4], dividends[i + 5]));
    }
  }

  private static Dynamic createDynamic(int[] divisors) {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup()
          .defineHiddenClassWithClassData(classBytes, divisors, true);
      Class<?> impl = lookup.lookupClass();
      MethodType at = MethodType.methodType(int.class, int.class);
      MethodType bt = at.appendParameterTypes(int.class);
      MethodType ct = bt.appendParameterTypes(int.class);
      MethodType dt = ct.appendParameterTypes(int.class);
      MethodType et = dt.appendParameterTypes(int.class);
      MethodType ft = et.appendParameterTypes(int.class);
      String n = "divide";
      return new Dynamic(
          lookup.findStatic(impl, n, at),
          lookup.findStatic(impl, n, bt),
          lookup.findStatic(impl, n, ct),
          lookup.findStatic(impl, n, dt),
          lookup.findStatic(impl, n, et),
          lookup.findStatic(impl, n, ft)
      );
    } catch (IllegalAccessException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  interface Division {
    int divide(int a, int b, int c, int d, int e, int f);

    int divide(int a, int b, int c, int d, int e);

    int divide(int a, int b, int c, int d);

    int divide(int a, int b, int c);

    int divide(int a, int b);

    int divide(int a);
  }

  record Basic(int a, int b, int c, int d, int e, int f) implements Division {
    @Override
    public int divide(int a, int b, int c, int d, int e, int f) {
      return a / this.a + b / this.b + c / this.c + d / this.d + e / this.e + f / this.f;
    }

    @Override
    public int divide(int a, int b, int c, int d, int e) {
      return a / this.a + b / this.b + c / this.c + d / this.d + e / this.e;
    }

    @Override
    public int divide(int a, int b, int c, int d) {
      return a / this.a + b / this.b + c / this.c + d / this.d;
    }

    @Override
    public int divide(int a, int b, int c) {
      return a / this.a + b / this.b + c / this.c;
    }

    @Override
    public int divide(int a, int b) {
      return a / this.a + b / this.b;
    }

    @Override
    public int divide(int a) {
      return a / this.a;
    }
  }

  record Handcraft(
      Divisor a,
      Divisor b,
      Divisor c,
      Divisor d,
      Divisor e,
      Divisor f
  ) implements Division {

    @Override
    public int divide(int a, int b, int c, int d, int e, int f) {
      return this.a.divide(a) + this.b.divide(b) + this.c.divide(c) + this.d.divide(d) + this.e.divide(e) + this.f.divide(f);
    }

    @Override
    public int divide(int a, int b, int c, int d, int e) {
      return this.a.divide(a) + this.b.divide(b) + this.c.divide(c) + this.d.divide(d) + this.e.divide(e);
    }

    @Override
    public int divide(int a, int b, int c, int d) {
      return this.a.divide(a) + this.b.divide(b) + this.c.divide(c) + this.d.divide(d);
    }

    @Override
    public int divide(int a, int b, int c) {
      return this.a.divide(a) + this.b.divide(b) + this.c.divide(c);
    }

    @Override
    public int divide(int a, int b) {
      return this.a.divide(a) + this.b.divide(b);
    }

    @Override
    public int divide(int a) {
      return this.a.divide(a);
    }
  }

  record Dynamic(
      MethodHandle a,
      MethodHandle b,
      MethodHandle c,
      MethodHandle d,
      MethodHandle e,
      MethodHandle f
  ) implements Division {

    @Override
    public int divide(int a, int b, int c, int d, int e, int f) {
      try {
        return (int) this.f.invokeExact(a, b, c, d, e, f);
      } catch (Throwable ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public int divide(int a, int b, int c, int d, int e) {
      try {
        return (int) this.e.invokeExact(a, b, c, d, e);
      } catch (Throwable ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public int divide(int a, int b, int c, int d) {
      try {
        return (int) this.d.invokeExact(a, b, c, d);
      } catch (Throwable ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public int divide(int a, int b, int c) {
      try {
        return (int) this.c.invokeExact(a, b, c);
      } catch (Throwable ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public int divide(int a, int b) {
      try {
        return (int) this.b.invokeExact(a, b);
      } catch (Throwable ex) {
        throw new RuntimeException(ex);
      }
    }

    @Override
    public int divide(int a) {
      try {
        return (int) this.a.invokeExact(a);
      } catch (Throwable ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  @SuppressWarnings("unused") // we require the bytecode of this template class
  private static class CalculateIndexBase {
    private static final int A;
    private static final int B;
    private static final int C;
    private static final int D;
    private static final int E;
    private static final int F;

    static {
      int[] data;
      try {
        data = MethodHandles.classData(MethodHandles.lookup(), ConstantDescs.DEFAULT_NAME, int[].class);
      } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
      A = data[0];
      B = data[1];
      C = data[2];
      D = data[3];
      E = data[4];
      F = data[5];
    }


    public static int divide(int a, int b, int c, int d, int e, int f) {
      return a / A + b / B + c / C + d / D + e / E + f / F;
    }


    public static int divide(int a, int b, int c, int d, int e) {
      return a / A + b / B + c / C + d / D + e / E;
    }


    public static int divide(int a, int b, int c, int d) {
      return a / A + b / B + c / C + d / D;
    }


    public static int divide(int a, int b, int c) {
      return a / A + b / B + c / C;
    }


    public static int divide(int a, int b) {
      return a / A + b / B;
    }


    public static int divide(int a) {
      return a / A;
    }
  }

  interface Divisor {

    static Divisor forDivisor(int divisor) {
      int abs = Math.abs(divisor);
      int k = 31 - Integer.numberOfLeadingZeros(abs);
      if (1 << k == abs) {
        return new ShiftDivisor(divisor);
      }
      return new MagicDivisor(divisor);
    }

    int divide(int dividend);
  }

  static class ShiftDivisor implements Divisor {
    private final int shift;
    private final boolean isNegative;

    ShiftDivisor(int divisor) {
      this.isNegative = divisor < 0;
      this.shift = 31 - Integer.numberOfLeadingZeros(Math.abs(divisor));
    }

    @Override
    public int divide(int dividend) {
      if (shift == 0) return dividend;
      int i = dividend >> (shift - 1);
      i >>>= 32 - shift;
      i += dividend;
      i >>= shift;
      if (isNegative) {
        return -i;
      }
      return i;
    }
  }

  static class MagicDivisor implements Divisor {
    private final int magicConst;
    private final int shiftConst;
    private final boolean isNegative;


    MagicDivisor(int divisor) {
      MagicDiv magic = magic(Math.abs(divisor));
      this.magicConst = magic.magicConst();
      this.shiftConst = magic.shiftConst();
      this.isNegative = divisor < 0;
    }

    @Override
    public int divide(int dividend) {
      long mulHi = Math.multiplyFull(magicConst, dividend);
      int r = (int) (mulHi >> 32);
      if (magicConst < 0) {
        r += dividend;
      }
      r >>= shiftConst;
      int o = dividend >> 31;
      if (isNegative) {
        return o - r;
      }
      return r - o;
    }
  }

  // Hacker's Delight, 2nd Edition, Figure 10-1, rewritten in Java
  // using long for "unsigned" ints
  private static MagicDiv magic(int d) {
    long two31 = 0x80000000L;
    int ad = Math.abs(d);
    long t = two31 + (d >>> 31);
    long anc = t - 1 - t % ad;
    long q1 = two31 / anc;
    long r1 = two31 - q1 * anc;
    long q2 = two31 / ad;
    long r2 = two31 - q2 * ad;
    int p = 31;
    long delta;
    do {
      p++;
      q1 *= 2;
      r1 *= 2;
      if (r1 >= anc) {
        q1++;
        r1 -= anc;
      }
      q2 *= 2;
      r2 *= 2;
      if (r2 >= ad) {
        q2++;
        r2 -= ad;
      }
      delta = ad - r2;
    } while (q1 < delta || (q1 == delta && r1 == 0));
    long M = q2 + 1;
    if (d < 0) {
      M = -M;
    }
    return new MagicDiv((int) M, p - 32);
  }

  private record MagicDiv(
      int magicConst,
      int shiftConst
  ) {

  }
}

