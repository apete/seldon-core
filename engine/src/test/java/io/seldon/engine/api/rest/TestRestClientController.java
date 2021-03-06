/*******************************************************************************
 * Copyright 2017 Seldon Technologies Ltd (http://www.seldon.io/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package io.seldon.engine.api.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.seldon.engine.filters.XSSFilter;
import io.seldon.engine.pb.ProtoBufUtils;
import io.seldon.engine.tracing.TracingProvider;
import io.seldon.protos.PredictionProtos.SeldonMessage;
import io.opentracing.mock.MockTracer;
import io.opentracing.mock.MockSpan;
import javax.servlet.http.HttpServletResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
// @AutoConfigureMockMvc
public class TestRestClientController {
	private final static Logger logger = LoggerFactory.getLogger(TestRestClientController.class);
	
	@Autowired
	private WebApplicationContext context;

  @MockBean
  private TracingProvider mockTracingProvider;

  private MockTracer tracer;
	
    //@Autowired
    private MockMvc mvc;

    @Before
	public void setup() {
    tracer = new MockTracer();
    when(mockTracingProvider.getTracer()).thenReturn(tracer);
    when(mockTracingProvider.isActive()).thenReturn(true);
		mvc = MockMvcBuilders
				.webAppContextSetup(context)
        .addFilters(new XSSFilter())
				.build();
	}
    
    @LocalServerPort
    private int port;


    @Test
    public void testPing() throws Exception
    {
    	MvcResult res = mvc.perform(MockMvcRequestBuilders.get("/ping")).andReturn();
    	String response = res.getResponse().getContentAsString();
    	Assert.assertEquals("pong", response);
    	Assert.assertEquals(200, res.getResponse().getStatus());
    }

    @Test
    public void testSecurityHeaders() throws Exception
    {
    	MvcResult res = mvc.perform(MockMvcRequestBuilders.get("/ping")).andReturn();
    	HttpServletResponse response = res.getResponse();

      final String noSniff = response.getHeader("X-Content-Type-Options");
    	Assert.assertEquals("nosniff", noSniff);
    	Assert.assertEquals(200, response.getStatus());
    }
    
    @Test
    public void testPredict_activateSpan() throws Exception
    {
        final String predictJson = "{" +
        	    "\"request\": {" + 
        	    "\"ndarray\": [[1.0]]}" +
        		"}";

    	MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
    			.accept(MediaType.APPLICATION_JSON_UTF8)
    			.content(predictJson)
    			.contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();
      List<MockSpan> finishedSpans = tracer.finishedSpans();

      Assert.assertEquals(1, finishedSpans.size());
    }
    
    @Test
    public void testPredict_11dim_ndarry() throws Exception
    {
        final String predictJson = "{" +
        	    "\"request\": {" + 
        	    "\"ndarray\": [[1.0]]}" +
        		"}";

    	MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
    			.accept(MediaType.APPLICATION_JSON_UTF8)
    			.content(predictJson)
    			.contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();
    	String response = res.getResponse().getContentAsString();
    	System.out.println(response);
    	Assert.assertEquals(200, res.getResponse().getStatus());
    }
    	    
    @Test
    public void testPredict_21dim_ndarry() throws Exception
    {
        final String predictJson = "{" +
        	    "\"request\": {" + 
        	    "\"ndarray\": [[1.0],[2.0]]}" +
        		"}";

    	MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
    			.accept(MediaType.APPLICATION_JSON_UTF8)
    			.content(predictJson)
    			.contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();
    	String response = res.getResponse().getContentAsString();
    	System.out.println(response);
    	Assert.assertEquals(200, res.getResponse().getStatus());
    	SeldonMessage.Builder builder = SeldonMessage.newBuilder();
		ProtoBufUtils.updateMessageBuilderFromJson(builder, response );
		SeldonMessage seldonMessage = builder.build();
		Assert.assertEquals(3, seldonMessage.getMeta().getMetricsCount());
		Assert.assertEquals("COUNTER", seldonMessage.getMeta().getMetrics(0).getType().toString());
		Assert.assertEquals("GAUGE", seldonMessage.getMeta().getMetrics(1).getType().toString());
		Assert.assertEquals("TIMER", seldonMessage.getMeta().getMetrics(2).getType().toString());
    }
    
    @Test
    public void testPredict_21dim_tensor() throws Exception
    {
        final String predictJson = "{" +
        	    "\"request\": {" + 
        	    "\"tensor\": {\"shape\":[2,1],\"values\":[1.0,2.0]}}" +
        		"}";

    	MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
    			.accept(MediaType.APPLICATION_JSON_UTF8)
    			.content(predictJson)
    			.contentType(MediaType.APPLICATION_JSON_UTF8)).andReturn();
    	String response = res.getResponse().getContentAsString();
    	System.out.println(response);
    	Assert.assertEquals(200, res.getResponse().getStatus());
    	SeldonMessage.Builder builder = SeldonMessage.newBuilder();
		ProtoBufUtils.updateMessageBuilderFromJson(builder, response );
		SeldonMessage seldonMessage = builder.build();
		Assert.assertEquals(3, seldonMessage.getMeta().getMetricsCount());
		Assert.assertEquals("COUNTER", seldonMessage.getMeta().getMetrics(0).getType().toString());
		Assert.assertEquals("GAUGE", seldonMessage.getMeta().getMetrics(1).getType().toString());
		Assert.assertEquals("TIMER", seldonMessage.getMeta().getMetrics(2).getType().toString());
    }

	@Test
	public void testPredict_multiform_11dim_ndarry() throws Exception
	{
		final String predictJson = "{" +
				"\"request\": {" +
				"\"ndarray\": [[1.0]]}" +
				"}";
		final MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
		paramMap.put("data", Arrays.asList(predictJson));
		MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.params(paramMap)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andReturn();
		String response = res.getResponse().getContentAsString();
		System.out.println(response);
		Assert.assertEquals(200, res.getResponse().getStatus());
	}

	@Test
	public void testPredict_multiform_21dim_ndarry() throws Exception
	{
		final String predictJson = "{" +
				"\"request\": {" +
				"\"ndarray\": [[1.0],[2.0]]}" +
				"}";
		final MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
		paramMap.put("data", Arrays.asList(predictJson));
		MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.params(paramMap)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andReturn();
		String response = res.getResponse().getContentAsString();
		System.out.println(response);
		Assert.assertEquals(200, res.getResponse().getStatus());
		SeldonMessage.Builder builder = SeldonMessage.newBuilder();
		ProtoBufUtils.updateMessageBuilderFromJson(builder, response );
		SeldonMessage seldonMessage = builder.build();
		Assert.assertEquals(3, seldonMessage.getMeta().getMetricsCount());
		Assert.assertEquals("COUNTER", seldonMessage.getMeta().getMetrics(0).getType().toString());
		Assert.assertEquals("GAUGE", seldonMessage.getMeta().getMetrics(1).getType().toString());
		Assert.assertEquals("TIMER", seldonMessage.getMeta().getMetrics(2).getType().toString());
	}

	@Test
	public void testPredict_multiform_21dim_tensor() throws Exception
	{
		final String predictJson = "{" +
				"\"request\": {" +
				"\"tensor\": {\"shape\":[2,1],\"values\":[1.0,2.0]}}" +
				"}";
		final MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
		paramMap.put("data", Arrays.asList(predictJson));
		MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.params(paramMap)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andReturn();
		String response = res.getResponse().getContentAsString();
		System.out.println(response);
		Assert.assertEquals(200, res.getResponse().getStatus());
		SeldonMessage.Builder builder = SeldonMessage.newBuilder();
		ProtoBufUtils.updateMessageBuilderFromJson(builder, response );
		SeldonMessage seldonMessage = builder.build();
		Assert.assertEquals(3, seldonMessage.getMeta().getMetricsCount());
		Assert.assertEquals("COUNTER", seldonMessage.getMeta().getMetrics(0).getType().toString());
		Assert.assertEquals("GAUGE", seldonMessage.getMeta().getMetrics(1).getType().toString());
		Assert.assertEquals("TIMER", seldonMessage.getMeta().getMetrics(2).getType().toString());
	}
	@Test
	public void testPredict_multiform_binData() throws Exception
	{
		final String metaJson =  "{\"puid\":\"1234\"}" ;
		final MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
		paramMap.put("meta", Arrays.asList(metaJson));
		byte[] fileData = "test data".getBytes();
		MvcResult res = mvc.perform(MockMvcRequestBuilders.fileUpload("/api/v0.1/predictions").file("binData",fileData)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.params(paramMap)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andReturn();
		String response = res.getResponse().getContentAsString();
		System.out.println(response);
		Assert.assertEquals(200, res.getResponse().getStatus());
		SeldonMessage.Builder builder = SeldonMessage.newBuilder();
		ProtoBufUtils.updateMessageBuilderFromJson(builder, response );
		SeldonMessage seldonMessage = builder.build();
		Assert.assertEquals(3, seldonMessage.getMeta().getMetricsCount());
		Assert.assertEquals("COUNTER", seldonMessage.getMeta().getMetrics(0).getType().toString());
		Assert.assertEquals("GAUGE", seldonMessage.getMeta().getMetrics(1).getType().toString());
		Assert.assertEquals("TIMER", seldonMessage.getMeta().getMetrics(2).getType().toString());
		Assert.assertEquals(new String(fileData), seldonMessage.getBinData().toStringUtf8());
		Assert.assertEquals("1234", seldonMessage.getMeta().getPuid());
	}
	@Test
	public void testPredict_multiform_strData_as_file() throws Exception
	{
		final String metaJson =  "{\"puid\":\"1234\"}" ;
		final MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
		paramMap.put("meta", Arrays.asList(metaJson));
		byte[] fileData = "test data".getBytes();
		MvcResult res = mvc.perform(MockMvcRequestBuilders.fileUpload("/api/v0.1/predictions").file("strData",fileData)
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.params(paramMap)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andReturn();
		String response = res.getResponse().getContentAsString();
		System.out.println(response);
		Assert.assertEquals(200, res.getResponse().getStatus());
		SeldonMessage.Builder builder = SeldonMessage.newBuilder();
		ProtoBufUtils.updateMessageBuilderFromJson(builder, response );
		SeldonMessage seldonMessage = builder.build();
		Assert.assertEquals(3, seldonMessage.getMeta().getMetricsCount());
		Assert.assertEquals("COUNTER", seldonMessage.getMeta().getMetrics(0).getType().toString());
		Assert.assertEquals("GAUGE", seldonMessage.getMeta().getMetrics(1).getType().toString());
		Assert.assertEquals("TIMER", seldonMessage.getMeta().getMetrics(2).getType().toString());
		Assert.assertEquals(new String(fileData), seldonMessage.getStrData());
		Assert.assertEquals("1234", seldonMessage.getMeta().getPuid());

	}
	@Test
	public void testPredict_multiform_strData_as_text() throws Exception
	{
		final String metaJson =  "{\"puid\":\"1234\"}" ;
		final MultiValueMap<String,String> paramMap = new LinkedMultiValueMap<>();
		paramMap.put("meta", Arrays.asList(metaJson));
		String strdata = "test data";
		paramMap.put("strData",Arrays.asList(strdata));
		MvcResult res = mvc.perform(MockMvcRequestBuilders.post("/api/v0.1/predictions")
				.accept(MediaType.APPLICATION_JSON_UTF8)
				.params(paramMap)
				.contentType(MediaType.MULTIPART_FORM_DATA)).andReturn();
		String response = res.getResponse().getContentAsString();
		System.out.println(response);
		Assert.assertEquals(200, res.getResponse().getStatus());
		SeldonMessage.Builder builder = SeldonMessage.newBuilder();
		ProtoBufUtils.updateMessageBuilderFromJson(builder, response );
		SeldonMessage seldonMessage = builder.build();
		Assert.assertEquals(3, seldonMessage.getMeta().getMetricsCount());
		Assert.assertEquals("COUNTER", seldonMessage.getMeta().getMetrics(0).getType().toString());
		Assert.assertEquals("GAUGE", seldonMessage.getMeta().getMetrics(1).getType().toString());
		Assert.assertEquals("TIMER", seldonMessage.getMeta().getMetrics(2).getType().toString());
		Assert.assertEquals(strdata, seldonMessage.getStrData());
		Assert.assertEquals("1234", seldonMessage.getMeta().getPuid());
	}
}
