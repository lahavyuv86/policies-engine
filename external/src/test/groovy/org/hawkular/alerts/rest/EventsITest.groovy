package org.hawkular.alerts.rest

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder
import groovy.json.JsonOutput
import io.quarkus.test.junit.QuarkusTest
import org.hawkular.alerts.api.json.JsonUtil
import org.hawkular.alerts.api.model.event.Event
import org.junit.jupiter.api.Test

import static org.hawkular.alerts.api.json.JsonUtil.fromJson
import static org.hawkular.alerts.api.json.JsonUtil.toJson
import static org.junit.jupiter.api.Assertions.assertEquals

/**
 * Events REST tests.
 *
 * @author Lucas Ponce
 */
@QuarkusTest
class EventsITest extends AbstractQuarkusITestBase {

    @Test
    void findEvents() {
        def resp = client.get(path: "events")
        assertEquals(200, resp.status)
    }

    @Test
    void findEventsByCriteria() {
        String now = String.valueOf(System.currentTimeMillis());
        def resp = client.get(path: "events", query: [endTime:now, startTime:"0",triggerIds:"Trigger-01,Trigger-02"] )
        assertEquals(200, resp.status)

        resp = client.get(path: "events",
                          query: [endTime:now, startTime:"0",eventIds:"Trigger-01|"+now+","+"Trigger-02|"+now] )
        assertEquals(200, resp.status)

        resp = client.get(path: "events", query: [endTime:now, startTime:"0",categories:"ALERT,LOG"] )
        assertEquals(200, resp.status)

        resp = client.get(path: "events", query: [tagQuery: "tags.tag-01 matches '*' and tags.tag-02 matches '*'"] )
        assertEquals(200, resp.status)

        resp = client.get(path: "events", query: [tagQuery:"tags.tag-01 = 'value-01' and tags.tag-02 = 'value-02'",thin:true] )
        assertEquals(200, resp.status)

        resp = client.get(path: "events", query: [tagQuery:"tags.tagA or (tags.tagB and tags.tagC in ['e.*', 'f.*'])"] )
        assertEquals(200, resp.status)
    }

    @Test
    void deleteEvents() {
        String now = String.valueOf(System.currentTimeMillis());

        def resp = client.delete(path: "events/badEventId" )
        assertEquals(404, resp.status)

        resp = client.put(path: "events/delete",
                          query: [endTime:now, startTime:"0",triggerIds:"Trigger-01,Trigger-02"] )
        assertEquals(200, resp.status)

        resp = client.put(path: "events/delete",
                          query: [endTime:now, startTime:"0", eventIds:"Trigger-01|"+now+","+"Trigger-02|"+now] )
        assertEquals(200, resp.status)

        resp = client.put(path: "events/delete",
                          query: [endTime:now, startTime:"0",categories:"A,B,C"] )
        assertEquals(200, resp.status)

        resp = client.put(path: "events/delete", query: [tagQuery:"tags.tag-01 matches '*' and tags.tag-02 matches '*'"] )
        assertEquals(200, resp.status)

        resp = client.put(path: "events/delete", query: [tagQuery:"tags.tag-01 = 'value-01' and tags.tag-02 = 'value-02'"] )
        assertEquals(200, resp.status)

        resp = client.put(path: "events/delete", query: [tagQuery:"tags.tagA or (tags.tagB and tags.tagC in ['e.*', 'f.*'])"] )
        assertEquals(200, resp.status)
    }

    @Test
    void createEvent() {
        String now = String.valueOf(System.currentTimeMillis());

        Map context = new java.util.HashMap();
        context.put("event-context-name", "event-context-value");

        Multimap<String, String> tags = MultimapBuilder.hashKeys().hashSetValues().build();
        tags.put("event-tag-name", "event-tag-value");
        Event event = new Event("test-tenant", "test-event-id", System.currentTimeMillis(), "test-event-data-id",
                "test-category", "test event text", context, tags);

        client.delete(path: "events/test-event-id" )

        def jsonBody = toJson(event)
        def resp = client.post(path: "events", body: jsonBody )
        assertEquals(200, resp.status)
        def jsonOut = JsonOutput.toJson(resp.data)
        event = fromJson(jsonOut, Event.class)
        assertEquals("test-event-id", event.getId())

        resp = client.post(path: "events", body: event )
        assertEquals(400, resp.status)

        resp = client.get(path: "events/event/test-event-id" )
        assertEquals(200, resp.status)

        jsonOut = JsonOutput.toJson(resp.data)
        Event e = fromJson(jsonOut, Event.class)
        assertEquals(event, e)
        assertEquals("test-category", e.getCategory())
        assertEquals("test event text", e.getText())
        assertEquals(context, e.getContext())
        assertEquals(tags, e.getTags())

        resp = client.get(path: "events", query: [startTime:now,tagQuery:"tags.event-tag-name = 'event-tag-value'"] )
        assertEquals(200, resp.status)
        assertEquals(1, resp.data.size)

        jsonOut = JsonOutput.toJson(resp.data)
        Event[] events = fromJson(jsonOut, Event[].class)

        e = events[0]

        resp = client.put(path: "events/tags", query: [eventIds:e.id,tags:"tag1name|tag1value"] )
        assertEquals(200, resp.status)

        resp = client.get(path: "events", query: [startTime:now,tagQuery:"tags.tag1name = 'tag1value'"] )
        assertEquals(200, resp.status)
        assertEquals(1, resp.data.size())

        jsonOut = JsonOutput.toJson(resp.data)
        events = fromJson(jsonOut, Event[].class)
        e = events[0]

        assertEquals(2, e.tags.size())
        assertEquals("event-tag-value", e.tags.get("event-tag-name").iterator().next())
        assertEquals("tag1value", e.tags.get("tag1name").iterator().next())

        resp = client.delete(path: "events/tags", query: [eventIds:e.id,tagNames:"tag1name"] )
        assertEquals(200, resp.status)

        resp = client.get(path: "events", query: [startTime:now,tagQuery:"tags.tag1name = 'tag1value'"] )
        assertEquals(200, resp.status)
        assertEquals(0, resp.data.size())

        resp = client.delete(path: "events/test-event-id" )
        assert resp.status == 200 : resp.status

        resp = client.get(path: "events/event/test-event-id" )
        assert resp.status == 404 : resp.status
    }

    @Test
    void sendAndNoPersistEvents() {
        String now = String.valueOf(System.currentTimeMillis());

        Event event = new Event("test-tenant", "test-event-id", System.currentTimeMillis(), "test-event-data-id",
                "test-category", "test event text");
        Collection<Event> events = Arrays.asList(event);

        def resp = client.post(path: "events/data", body: events )
        assertEquals(200, resp.status)

        resp = client.get(path: "events/event/test-event-id" )
        assertEquals(404, resp.status)

        resp = client.get(path: "events", query: [startTime:now] )
        assertEquals(200, resp.status)
        assertEquals(0, resp.data.size())
    }

    @Test
    void testQueryEventsWithSpacesInValues() {
        def cTime = System.currentTimeMillis();
        Event e1 = new Event();
        e1.setId("test_1");
        e1.setCtime(cTime - 4000);
        e1.setCategory("test");
        e1.setText("Avail-changed:[UP] WildFly Server");
        e1.getContext().put("resource_path", "/t;hawkular/f;my-agent/r;Local%20DMR~~");
        e1.getContext().put("message", "Avail-changed:[UP] WildFly Server");
        e1.getTags().put("test_tag", "/t;hawkular/f;my-agent/r;Local%20DMR~~_Server Availability");

        def resp = client.post(path: "events", body: e1)
        assertEquals(200, resp.status)
        def event = resp.data
        assertEquals("test_1", event.id)

        Event e2 = new Event();
        e2.setId("test_2");
        e2.setCtime(cTime);
        e2.setCategory("test");
        e2.setText("Avail-changed:[DOWN] Deployment");
        e2.getContext().put("resource_path", "/t;hawkular/f;my-agent/r;Local%20DMR~%2Fdeployment%3Dcfme_test_ear_middleware.ear");
        e2.getContext().put("message", "Avail-changed:[DOWN] Deployment");
        e2.getTags().put("test_tag", "/t;hawkular/f;my-agent/r;Local%20DMR~%2Fdeployment%3Dcfme_test_ear_middleware.ear_Deployment Status");

        def jsonBody = toJson(e2)
        resp = client.post(path: "events", body: jsonBody)
        assertEquals(200, resp.status)
        event = resp.data
        assertEquals("test_2", event.id)

        def tagQuery = "tags.test_tag = '/t;hawkular/f;my-agent/r;Local%20DMR~~_Server Availability'"

        resp = client.get(path: "events", query: [tagQuery: tagQuery] )
        assertEquals(200, resp.status)
        assertEquals(1, resp.data.size())
    }

    // HWKALERTS-275
    @Test
    void testCreateAndQueryEvents() {
        def numEvents = 1000
        def resp
        for (int i = 0; i < numEvents; i++) {
            Event eventX = new Event()
            eventX.setId("event" + i)
            eventX.setCategory("test")
            eventX.setText("Event message " + i)
            eventX.getTags().put("tag" + (i % 3), "value" + (i % 3))

            def jsonBody = toJson(eventX)
            resp = client.post(path: "events", body: jsonBody)
            assertEquals(200, resp.status)
        }

        def tagQuery = "tags.tag0"
        resp = client.get(path: "events", query: [tagQuery: tagQuery])
        assertEquals(200, resp.status)
        assertEquals(334, resp.data.size())

        tagQuery = "tags.tag1"
        resp = client.get(path: "events", query: [tagQuery: tagQuery])
        assertEquals(200, resp.status)
        assertEquals(333, resp.data.size())

        tagQuery = "tags.tag2"
        resp = client.get(path: "events", query: [tagQuery: tagQuery])
        assertEquals(200, resp.status)
        assertEquals(333, resp.data.size())

        def eventIds = ""
        for (int i = 0; i < 199; i++) {
            eventIds += "event" + i + ",";
        }
        eventIds += "event199";

        resp = client.get(path: "events", query: [eventIds: eventIds])
        assertEquals(200, resp.status)
        assertEquals(200, resp.data.size())
    }
}
