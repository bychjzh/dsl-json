package com.dslplatform.json;

import com.dslplatform.json.runtime.Settings;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ObjectFormatTest {

	@CompiledJson(formats = CompiledJson.Format.OBJECT)
	public static class Composite {
		@JsonAttribute(index = 3)
		public int[] x;
		@JsonAttribute(index = 2)
		public List<String> s;
		@JsonAttribute(index = 1)
		public Double d;
	}

	@CompiledJson(formats = CompiledJson.Format.OBJECT)
	public static class ImmutableComposite {
		@JsonAttribute(index = 3)
		public final int[] x;
		@JsonAttribute(index = 2)
		public final List<String> s;
		@JsonAttribute(index = 1)
		public final Double d;

		public ImmutableComposite(int[] x, List<String> s, Double d) {
			this.x = x;
			this.s = s;
			this.d = d;
		}
	}

	@CompiledJson(formats = CompiledJson.Format.OBJECT)
	public static class NoProps {
	}

	@CompiledJson(formats = CompiledJson.Format.OBJECT)
	public static class SingleExcludedProp {
		public int x;
	}

	@CompiledJson(formats = CompiledJson.Format.OBJECT, discriminator = "include")
	public static class NoPropsWithDiscriminator {
	}

	@CompiledJson(formats = CompiledJson.Format.OBJECT, discriminator = "always", onUnknown = CompiledJson.Behavior.FAIL)
	public static class SingleExcludedPropWithDiscriminator {
		public int x;
	}

	private final DslJson<Object> dslJsonFull = new DslJson<>(Settings.withRuntime().allowArrayFormat(true).includeServiceLoader());
	private final DslJson<Object> dslJsonMinimal = new DslJson<>(Settings.withRuntime().allowArrayFormat(true).skipDefaultValues(true).includeServiceLoader());

	private final DslJson<Object>[] dslJsons = new DslJson[]{dslJsonFull, dslJsonMinimal};

	@Test
	public void objectRoundtrip() throws IOException {
		for (DslJson<Object> dslJson : dslJsons) {
			Composite c = new Composite();
			c.d = Double.parseDouble("123.456");
			c.s = Arrays.asList("abc", "def", null, "ghi");
			c.x = new int[]{1, -1, -0};
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			dslJson.serialize(c, os);
			Assert.assertEquals("{\"d\":123.456,\"s\":[\"abc\",\"def\",null,\"ghi\"],\"x\":[1,-1,0]}", os.toString());
			Composite res = dslJson.deserialize(Composite.class, os.toByteArray(), os.size());
			Assert.assertEquals(c.d, res.d);
			Assert.assertEquals(c.s, res.s);
			Assert.assertArrayEquals(c.x, res.x);
		}
	}

	@Test
	public void immutableRoundtrip() throws IOException {
		for (DslJson<Object> dslJson : dslJsons) {
			ImmutableComposite c = new ImmutableComposite(
					new int[]{1, -1, -0},
					Arrays.asList("abc", "def", null, "ghi"),
					Double.parseDouble("123.456")
			);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			dslJson.serialize(c, os);
			Assert.assertEquals("{\"d\":123.456,\"s\":[\"abc\",\"def\",null,\"ghi\"],\"x\":[1,-1,0]}", os.toString());
			ImmutableComposite res = dslJson.deserialize(ImmutableComposite.class, os.toByteArray(), os.size());
			Assert.assertEquals(c.d, res.d);
			Assert.assertEquals(c.s, res.s);
			Assert.assertArrayEquals(c.x, res.x);
		}
	}

	@Test
	public void noProperties() throws IOException {
		for (DslJson<Object> dslJson : dslJsons) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			dslJson.serialize(new NoProps(), os);
			Assert.assertEquals("{}", os.toString());
			NoProps res = dslJson.deserialize(NoProps.class, os.toByteArray(), os.size());
			Assert.assertNotNull(res);
		}
	}

	@Test
	public void singleExcluded() throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		dslJsonMinimal.serialize(new SingleExcludedProp(), os);
		Assert.assertEquals("{}", os.toString());
		SingleExcludedProp res = dslJsonMinimal.deserialize(SingleExcludedProp.class, os.toByteArray(), os.size());
		Assert.assertEquals(0, res.x);
	}

	@Test
	public void noPropertiesWithDiscriminator() throws IOException {
		for (DslJson<Object> dslJson : dslJsons) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			dslJson.serialize(new NoPropsWithDiscriminator(), os);
			Assert.assertEquals("{\"include\":\"com.dslplatform.json.ObjectFormatTest.NoPropsWithDiscriminator\"}", os.toString());
			NoPropsWithDiscriminator res = dslJson.deserialize(NoPropsWithDiscriminator.class, os.toByteArray(), os.size());
			Assert.assertNotNull(res);
		}
	}

	@Test
	public void singleExcludedWithDiscriminator() throws IOException {
		for (DslJson<Object> dslJson : dslJsons) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			dslJson.serialize(new SingleExcludedPropWithDiscriminator(), os);
			Assert.assertTrue(os.toString().startsWith("{\"always\":\"com.dslplatform.json.ObjectFormatTest.SingleExcludedPropWithDiscriminator\""));
			SingleExcludedPropWithDiscriminator res = dslJson.deserialize(SingleExcludedPropWithDiscriminator.class, os.toByteArray(), os.size());
			Assert.assertNotNull(res);
		}
	}

	public static class UppercaseName {
		private int doc;
		public int getDOC() { return doc; }
		public void setDOC(int value) { doc = value; }
	}

	@Test
	public void uppercaseName() throws IOException {
		UppercaseName val = new UppercaseName();
		val.setDOC(505);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		dslJsonFull.serialize(val, baos);
		Assert.assertEquals("{\"DOC\":505}", baos.toString("UTF-8"));
		byte[] bytes = baos.toByteArray();
		UppercaseName deser = dslJsonFull.deserialize(UppercaseName.class, bytes, bytes.length);
		Assert.assertEquals(val.getDOC(), deser.getDOC());
	}

	@CompiledJson
	public static class EmptyClass {
	}

	@CompiledJson
	public static class UnusedProperty {
		public int x;
	}

	@Test
	public void classWithoutProperties() throws IOException {
		byte[] bytes = "{\"p1\":123,\"p2\":\"abc\"}".getBytes("UTF-8");
		EmptyClass deser = dslJsonFull.deserialize(EmptyClass.class, bytes, bytes.length);
		Assert.assertNotNull(deser);
	}

	@Test
	public void classWithUnusedProperty() throws IOException {
		byte[] bytes = "{\"p1\":123,\"p2\":\"abc\"}".getBytes("UTF-8");
		UnusedProperty deser = dslJsonFull.deserialize(UnusedProperty.class, bytes, bytes.length);
		Assert.assertNotNull(deser);
	}

	@Test
	public void classWithUsedProperty() throws IOException {
		byte[] bytes = "{\"x\":505,\"p1\":123,\"p2\":\"abc\"}".getBytes("UTF-8");
		UnusedProperty deser = dslJsonFull.deserialize(UnusedProperty.class, bytes, bytes.length);
		Assert.assertEquals(505, deser.x);
	}

	@CompiledJson
	public static class WithMandatory {
		@JsonAttribute(name = "guids", index = 1)
		public UUID[] uuids;
		@JsonAttribute(ignore = true)
		public String ignore;
		@JsonAttribute(mandatory = true, index = 2)
		public List<Response> codes;
	}

	public enum Response { Ok, BadRequest }

	@Test
	public void withMandatory() throws IOException {
		WithMandatory m = new WithMandatory();
		UUID id1 = UUID.randomUUID();
		UUID id2 = UUID.randomUUID();
		m.uuids = new UUID[] { id1, id2 };
		m.codes = Arrays.asList(Response.BadRequest, Response.Ok);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		dslJsonFull.serialize(m, baos);
		Assert.assertEquals("{\"guids\":[\"" + id1 + "\",\"" + id2 + "\"],\"codes\":[\"BadRequest\",\"Ok\"]}", baos.toString("UTF-8"));
		byte[] bytes = baos.toByteArray();
		WithMandatory deser = dslJsonFull.deserialize(WithMandatory.class, bytes, bytes.length);
		Assert.assertEquals(2, deser.uuids.length);
		Assert.assertEquals(id1, deser.uuids[0]);
		Assert.assertEquals(id2, deser.uuids[1]);
		Assert.assertEquals(m.codes, deser.codes);
	}
}
