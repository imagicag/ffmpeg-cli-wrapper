package net.bramp.ffmpeg.modelmapper;

import java.util.Objects;
import org.modelmapper.Condition;
import org.modelmapper.spi.MappingContext;

/**
 * Only maps properties which are not their type's default value.
 *
 * @param <S> source type
 * @param <D> destination type
 * @author bramp
 */
public class NotDefaultCondition<S, D> implements Condition<S, D> {

  public static final NotDefaultCondition<Object, Object> notDefault = new NotDefaultCondition<>();

  @Override
  public boolean applies(MappingContext<S, D> context) {
    return !Objects.equals(context.getSource(), defaultValue(context.getSourceType()));
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) return null;
    if (type == boolean.class) return false;
    if (type == char.class) return '\0';
    if (type == byte.class) return (byte) 0;
    if (type == short.class) return (short) 0;
    if (type == int.class) return 0;
    if (type == long.class) return 0L;
    if (type == float.class) return 0F;
    if (type == double.class) return 0D;
    throw new AssertionError("Unknown primitive type: " + type);
  }
}
