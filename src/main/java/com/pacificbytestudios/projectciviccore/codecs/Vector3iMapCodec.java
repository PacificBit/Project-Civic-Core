package com.pacificbytestudios.projectciviccore.codecs;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nonnull;

import org.bson.BsonDocument;
import org.bson.BsonValue;
import org.joml.Vector3i;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.WrappedCodec;
import com.hypixel.hytale.codec.exception.CodecException;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.util.RawJsonReader;

public class Vector3iMapCodec<V, M extends Map<Vector3i, V>>
    implements Codec<Map<Vector3i, V>>, WrappedCodec<V> {
  private final Codec<V> valueCodec;
  private final Supplier<M> supplier;
  private final boolean unmodifiable;

  public Vector3iMapCodec(Codec<V> valueCodec, Supplier<M> supplier, boolean unmodifiable) {
    this.valueCodec = valueCodec;
    this.supplier = supplier;
    this.unmodifiable = unmodifiable;
  }

  public Vector3iMapCodec(Codec<V> valueCodec, Supplier<M> supplier) {
    this(valueCodec, supplier, true);
  }

  @Override
  public Codec<V> getChildCodec() {
    return this.valueCodec;
  }

  @Override
  public Map<Vector3i, V> decode(@Nonnull BsonValue bsonValue, @Nonnull ExtraInfo extraInfo) {
    BsonDocument bsonDocument = bsonValue.asDocument();
    Map<Vector3i, V> map = this.supplier.get();

    for (Map.Entry<String, BsonValue> entry : bsonDocument.entrySet()) {
      String key = entry.getKey();
      BsonValue value = entry.getValue();
      extraInfo.pushKey(key);
      try {
        map.put(decodeKey(key), this.valueCodec.decode(value, extraInfo));
      } catch (Exception e) {
        throw new CodecException("Failed to decode", value, extraInfo, e);
      } finally {
        extraInfo.popKey();
      }
    }

    if (this.unmodifiable) {
      map = Collections.unmodifiableMap(map);
    }

    return map;
  }

  @Override
  @Nonnull
  public BsonValue encode(@Nonnull Map<Vector3i, V> map, ExtraInfo extraInfo) {
    BsonDocument bsonDocument = new BsonDocument();

    for (Map.Entry<Vector3i, V> entry : map.entrySet()) {
      BsonValue value = this.valueCodec.encode(entry.getValue(), extraInfo);
      if (value == null || value.isNull() || value.isDocument() && value.asDocument().isEmpty()
          || value.isArray() && value.asArray().isEmpty()) {
        continue;
      }
      bsonDocument.put(encodeKey(entry.getKey()), value);
    }

    return bsonDocument;
  }

  @Override
  public Map<Vector3i, V> decodeJson(@Nonnull RawJsonReader reader, @Nonnull ExtraInfo extraInfo) throws IOException {
    reader.expect('{');
    reader.consumeWhiteSpace();

    if (reader.tryConsume('}')) {
      return this.unmodifiable ? Collections.emptyMap() : this.supplier.get();
    }

    Map<Vector3i, V> map = this.supplier.get();

    while (true) {
      String rawKey = reader.readString();
      Vector3i key = decodeKey(rawKey);
      reader.consumeWhiteSpace();
      reader.expect(':');
      reader.consumeWhiteSpace();
      extraInfo.pushKey(rawKey, reader);
      try {
        map.put(key, this.valueCodec.decodeJson(reader, extraInfo));
      } catch (Exception e) {
        throw new CodecException("Failed to decode", reader, extraInfo, e);
      } finally {
        extraInfo.popKey();
      }
      reader.consumeWhiteSpace();
      if (reader.tryConsumeOrExpect('}', ',')) {
        if (this.unmodifiable) {
          map = Collections.unmodifiableMap(map);
        }
        return map;
      }
      reader.consumeWhiteSpace();
    }
  }

  @Override
  @Nonnull
  public Schema toSchema(@Nonnull SchemaContext context) {
    ObjectSchema schema = new ObjectSchema();
    schema.setTitle("Vector3iMap");
    schema.setPropertyNames(new StringSchema());
    schema.setAdditionalProperties(context.refDefinition(this.valueCodec));
    return schema;
  }

  @Nonnull
  public static String encodeKey(@Nonnull Vector3i position) {
    return position.x + "," + position.y + "," + position.z;
  }

  @Nonnull
  public static Vector3i decodeKey(@Nonnull String raw) {
    String[] parts = raw.split(",", 3);
    if (parts.length != 3) {
      throw new IllegalArgumentException("Invalid Vector3i key: " + raw);
    }

    return new Vector3i(
        Integer.parseInt(parts[0]),
        Integer.parseInt(parts[1]),
        Integer.parseInt(parts[2]));
  }
}
