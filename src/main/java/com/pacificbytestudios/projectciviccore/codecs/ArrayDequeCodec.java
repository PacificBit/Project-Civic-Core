package com.pacificbytestudios.projectciviccore.codecs;

import java.util.ArrayDeque;

import org.bson.BsonArray;
import org.bson.BsonValue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ArraySchema;
import com.hypixel.hytale.codec.schema.config.Schema;

public final class ArrayDequeCodec<T> implements Codec<ArrayDeque<T>> {
  private final Codec<T> childCodec;
  private final String title;

  public ArrayDequeCodec(Codec<T> childCodec, String title) {
    this.childCodec = childCodec;
    this.title = title;
  }

  @Override
  public Schema toSchema(SchemaContext context) {
    ArraySchema schema = new ArraySchema();
    schema.setTitle(this.title);
    schema.setItem(this.childCodec.toSchema(context));
    return schema;
  }

  @Override
  public ArrayDeque<T> decode(BsonValue bsonValue, ExtraInfo info) {
    ArrayDeque<T> output = new ArrayDeque<>();
    if (Codec.isNullBsonValue(bsonValue)) {
      return output;
    }

    bsonValue.asArray().forEach(item -> output.addLast(this.childCodec.decode(item, info)));

    return output;
  }

  @Override
  public BsonValue encode(ArrayDeque<T> deque, ExtraInfo info) {
    BsonArray output = new BsonArray();
    if (deque != null) {
      deque.forEach(item -> output.add(this.childCodec.encode(item, info)));
    }
    return output;
  }
}
