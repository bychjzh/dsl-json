package com.dslplatform.json.runtime;

import com.dslplatform.json.ConfigurationException;
import com.dslplatform.json.JsonReader;
import com.dslplatform.json.Nullable;
import com.dslplatform.json.ParsingException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.concurrent.Callable;

public final class CollectionDecoder<E, T extends Collection<E>> implements JsonReader.ReadObject<T> {

	private final Type manifest;
	private final Callable<T> newInstance;
	private final JsonReader.ReadObject<E> decoder;

	public CollectionDecoder(
			final Type manifest,
			final Callable<T> newInstance,
			final JsonReader.ReadObject<E> decoder) {
		if (manifest == null) throw new IllegalArgumentException("manifest can't be null");
		if (newInstance == null) throw new IllegalArgumentException("create can't be null");
		if (decoder == null) throw new IllegalArgumentException("decoder can't be null");
		this.manifest = manifest;
		this.newInstance = newInstance;
		this.decoder = decoder;
	}

	@Nullable
	@Override
	public T read(final JsonReader reader) throws IOException {
		if (reader.wasNull()) return null;
		if (reader.last() != '[') {
			throw reader.newParseError("Expecting '[' for collection start");
		}
		final T instance;
		try {
			instance = newInstance.call();
		} catch (Exception e) {
			throw new ConfigurationException("Unable to create a new instance of " + manifest, e);
		}
		if (reader.getNextToken() == ']') return instance;
		instance.add(decoder.read(reader));
		while (reader.getNextToken() == ','){
			reader.getNextToken();
			instance.add(decoder.read(reader));
		}
		if (reader.last() != ']') {
			throw reader.newParseError("Expecting ']' for collection end");
		}
		return instance;
	}
}
