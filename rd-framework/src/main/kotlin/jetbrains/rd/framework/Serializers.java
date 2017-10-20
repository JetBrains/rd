package com.jetbrains.rider.framework;

import kotlin.Pair;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public final class Serializers extends SerializersBase {

  @Override
  public <T> T readPolymorphic(@NotNull SerializationCtx ctx, @NotNull InputStream stream) {
    RdId id = RdId.Companion.read(stream);
    if (id.isNull()) return null;

    HashMap<RdId, Function2<SerializationCtx, InputStream, Object>> readers = getReaders();

    Function2<? super SerializationCtx, ? super InputStream, ?> reader = readers.get(id);
    if (reader == null) {
      throw new IllegalStateException("Can't find reader by id: " + id.toString());
    }

    //noinspection unchecked
    return (T)reader.invoke(ctx, stream);
  }

  @Override
  public <T> void writePolymorphic(@NotNull SerializationCtx ctx, @NotNull OutputStream stream, T value) {
    if (value == null) {
        RdId.Companion.getNull().write(stream);
        return;
    }
    Pair<RdId, Function3<SerializationCtx, OutputStream, Object, Object>> p = getWriters().get(value.getClass());
    p.component1().write(stream);
    p.component2().invoke(ctx, stream, value);
  }
}
