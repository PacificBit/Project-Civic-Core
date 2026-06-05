package com.pacificbytestudios.projectciviccore.codecs;

import java.io.IOException;

import org.bson.BsonDocument;
import org.bson.BsonValue;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.ObjectSchema;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.util.RawJsonReader;

public final class BsonDocumentValueCodec implements Codec<BsonDocument> {
  @Override
  public BsonDocument decode(BsonValue value, ExtraInfo extraInfo) {
    return value.asDocument();
  }

  @Override
  public BsonValue encode(BsonDocument value, ExtraInfo extraInfo) {
    return value;
  }

  @Override
  public BsonDocument decodeJson(RawJsonReader reader, ExtraInfo extraInfo) throws IOException {
    reader.consumeWhiteSpace();
    reader.mark();
    try {
      reader.skipObject();
      return BsonDocument.parse(new String(reader.cloneMark()));
    } finally {
      reader.unmark();
    }
  }

  @Override
  public Schema toSchema(SchemaContext context) {
    return new ObjectSchema();
  }
}
