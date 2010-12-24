package com.tinkerpop.rexster;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.tinkerpop.blueprints.pgm.Edge;
import com.tinkerpop.blueprints.pgm.Graph;
import com.tinkerpop.blueprints.pgm.Vertex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

/**
 * Tests edge resource.  Should not need to test any specific returns values as they are 
 * covered under other unit tests.  The format of the results themselves should be covered 
 * under the ElementJSONObject.
 */
public class EdgeResourceTest {

	protected Mockery mockery = new JUnit4Mockery();
    protected final String baseUri = "http://localhost/mock";

    @Before
    public void init() {
        this.mockery = new JUnit4Mockery();
    }
    
    @Test
    public void getAllEdgesNoOffset(){
    	final int numberOfEdges = 100;
    	EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges);
    	
    	Response response = resource.getAllEdges();
    	this.assertEdgesOkResponseJsonStructure(numberOfEdges, numberOfEdges, response);
    }
    
    @Test
    public void getAllEdgesNoResults(){
    	final int numberOfEdges = 0;
    	EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges);
    	
    	Response response = resource.getAllEdges();
    	this.assertEdgesOkResponseJsonStructure(numberOfEdges, numberOfEdges, response);
    }
    
    @Test
    public void getAllEdgesWithValidOffset(){
    	final int numberOfEdges = 100;
    	HashMap<String, String> parameters = new HashMap<String, String>();
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
    	EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges, parameters);
    	
    	Response response = resource.getAllEdges();
    	this.assertEdgesOkResponseJsonStructure(10, numberOfEdges, response);
    	
    	JSONObject json = (JSONObject) response.getEntity();
    	JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
    	
    	// should return ids 10 through 19 from the random generated data
    	for (int ix = 0; ix < jsonResults.length(); ix++) {
    		Assert.assertEquals(ix + 10, jsonResults.optJSONObject(ix).optInt(Tokens._ID));
    	}
    }
    
    @Test
    public void getAllEdgesWithInvalidOffsetNotEnoughResults(){
    	final int numberOfEdges = 5;
    	HashMap<String, String> parameters = new HashMap<String, String>();
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "10");
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
    	EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges, parameters);
    	
    	Response response = resource.getAllEdges();
    	this.assertEdgesOkResponseJsonStructure(0, numberOfEdges, response);
    }
    
    @Test
    public void getAllEdgesWithInvalidOffsetStartAfterEnd(){
    	final int numberOfEdges = 5;
    	HashMap<String, String> parameters = new HashMap<String, String>();
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_START, "100");
    	parameters.put(Tokens.REXSTER + "." + Tokens.OFFSET_END, "20");
    	EdgeResource resource = this.constructMockGetAllEdgesScenario(numberOfEdges, parameters);
    	
    	Response response = resource.getAllEdges();
    	this.assertEdgesOkResponseJsonStructure(0, numberOfEdges, response);
    }
    
    @Test(expected = WebApplicationException.class)
    public void getSingleEdgeNotFound() {
    	EdgeResource resource = this.constructMockGetSingleEdgeScenario(null, new HashMap<String, String>());
    	resource.getSingleEdge("id-does-not-match-any");
    }
    
    @Test
    public void getSingleVertexFound() {

    	Vertex v1 = new MockVertex("1");
    	Vertex v2 = new MockVertex("2");
    	
    	Edge v = new MockEdge("1", "label-1", new Hashtable<String, Object>(), v1, v2);
    	EdgeResource resource = this.constructMockGetSingleEdgeScenario(v, new HashMap<String, String>());
    	
    	Response response = resource.getSingleEdge("1");
    	Assert.assertNotNull(response);
    	Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    	Assert.assertNotNull(response.getEntity());
    	Assert.assertTrue(response.getEntity() instanceof JSONObject);
    	
    	JSONObject json = (JSONObject) response.getEntity();
    	Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    	Assert.assertTrue(json.optDouble(Tokens.QUERY_TIME) > 0);
    	
    	Assert.assertTrue(json.has(Tokens.RESULTS));
    	Assert.assertFalse(json.isNull(Tokens.RESULTS));
    	
    	JSONObject jsonResult = (JSONObject) json.optJSONObject(Tokens.RESULTS);
    	Assert.assertNotNull(jsonResult);
    }
    
    private EdgeResource constructMockGetSingleEdgeScenario(final Edge edge, final HashMap<String, String> parameters){
    	final Graph graph = this.mockery.mock(Graph.class);
    	final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
    	
    	final UriInfo uri = this.mockery.mock(UriInfo.class);
    	
    	final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    	
    	this.mockery.checking(new Expectations() {{
    		allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdge(with(any(Object.class)));
            will(returnValue(edge));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});
    	
    	EdgeResource resource = new EdgeResource("graph", uri, httpServletRequest, rap);
		return resource;
    }
    
    private EdgeResource constructMockGetAllEdgesScenario(final int numberOfVertices) {
    	return this.constructMockGetAllEdgesScenario(numberOfVertices, new HashMap<String, String>());
    }
    
    private EdgeResource constructMockGetAllEdgesScenario(final int numberOfEdges, final HashMap<String, String> parameters) {
		final Graph graph = this.mockery.mock(Graph.class);
    	final RexsterApplicationGraph rag = new RexsterApplicationGraph("graph", graph);
        final RexsterApplicationProvider rap = this.mockery.mock(RexsterApplicationProvider.class);
    	
    	final UriInfo uri = this.mockery.mock(UriInfo.class);
    	
    	final HttpServletRequest httpServletRequest = this.mockery.mock(HttpServletRequest.class);
    	
    	this.mockery.checking(new Expectations() {{
    		allowing(httpServletRequest).getParameterMap();
            will(returnValue(parameters));
            allowing(graph).getEdges();
            will(returnValue(generateMockedEdges(numberOfEdges)));
            allowing(rap).getApplicationGraph(with(any(String.class)));
            will(returnValue(rag));
        }});
    	
    	EdgeResource resource = new EdgeResource("graph", uri, httpServletRequest, rap);
		return resource;
	}
    
    private void assertEdgesOkResponseJsonStructure(int numberOfEdgesReturned, 
    		int numberOfEdgesTotal, Response response) {
		Assert.assertNotNull(response);
    	Assert.assertEquals(Status.OK.getStatusCode(), response.getStatus());
    	Assert.assertNotNull(response.getEntity());
    	Assert.assertTrue(response.getEntity() instanceof JSONObject);
    	
    	JSONObject json = (JSONObject) response.getEntity();
    	Assert.assertTrue(json.has(Tokens.TOTAL_SIZE));
    	Assert.assertEquals(numberOfEdgesTotal, json.optInt(Tokens.TOTAL_SIZE));
    	Assert.assertTrue(json.has(Tokens.QUERY_TIME));
    	Assert.assertTrue(json.optDouble(Tokens.QUERY_TIME) > 0);
    	
    	Assert.assertTrue(json.has(Tokens.RESULTS));
    	Assert.assertFalse(json.isNull(Tokens.RESULTS));
    	
    	JSONArray jsonResults = json.optJSONArray(Tokens.RESULTS);
    	Assert.assertEquals(numberOfEdgesReturned, jsonResults.length());
	}
    
    private static Iterable<Edge> generateMockedEdges(int numberOfEdges) {
    	ArrayList<Edge> edges = new ArrayList<Edge>();
    	
    	MockVertex v1 = new MockVertex("1");
    	MockVertex v2 = new MockVertex("2");
    	
    	for (int ix = 0; ix < numberOfEdges; ix++) {
    		MockEdge e = new MockEdge(new Integer(ix).toString(), 
    				"label-" + new Integer(ix).toString(), new Hashtable<String, Object>(), v1, v2);
    		edges.add(e);
    	}
    	
    	return edges;
    }
}