package org.onlab.onos.net.flow.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_ADDED;
import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_REMOVED;
import static org.onlab.onos.net.flow.FlowRuleEvent.Type.RULE_UPDATED;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.ApplicationId;
import org.onlab.onos.event.impl.TestEventDispatcher;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.Device.Type;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.DefaultFlowRule;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRule.FlowRuleState;
import org.onlab.onos.net.flow.FlowRuleEvent;
import org.onlab.onos.net.flow.FlowRuleListener;
import org.onlab.onos.net.flow.FlowRuleProvider;
import org.onlab.onos.net.flow.FlowRuleProviderRegistry;
import org.onlab.onos.net.flow.FlowRuleProviderService;
import org.onlab.onos.net.flow.FlowRuleService;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.criteria.Criterion;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.trivial.impl.SimpleFlowRuleStore;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Test codifying the flow rule service & flow rule provider service contracts.
 */
public class FlowRuleManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID = DeviceId.deviceId("of:001");
    private static final Device DEV = new DefaultDevice(
            PID, DID, Type.SWITCH, "", "", "", "");

    private FlowRuleManager mgr;

    protected FlowRuleService service;
    protected FlowRuleProviderRegistry registry;
    protected FlowRuleProviderService providerService;
    protected TestProvider provider;
    protected TestListener listener = new TestListener();
    private ApplicationId appId;

    @Before
    public void setUp() {
        mgr = new FlowRuleManager();
        mgr.store = new SimpleFlowRuleStore();
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.deviceService = new TestDeviceService();
        service = mgr;
        registry = mgr;

        mgr.activate();
        mgr.addListener(listener);
        provider = new TestProvider(PID);
        providerService = registry.register(provider);
        appId = ApplicationId.getAppId();
        assertTrue("provider should be registered",
                registry.getProviders().contains(provider.id()));
    }

    @After
    public void tearDown() {
        registry.unregister(provider);
        assertFalse("provider should not be registered",
                registry.getProviders().contains(provider.id()));
        service.removeListener(listener);
        mgr.deactivate();
        mgr.eventDispatcher = null;
        mgr.deviceService = null;
    }

    private FlowRule flowRule(int tsval, int trval) {
        TestSelector ts = new TestSelector(tsval);
        TestTreatment tr = new TestTreatment(trval);
        return new DefaultFlowRule(DID, ts, tr, 0, appId);
    }

    private FlowRule flowRule(FlowRule rule, FlowRuleState state) {
        return new DefaultFlowRule(rule, state);
    }

    private FlowRule addFlowRule(int hval) {
        FlowRule rule = flowRule(hval, hval);
        providerService.flowAdded(rule);
        assertNotNull("rule should be found", service.getFlowEntries(DID));
        return rule;
    }

    private void validateEvents(FlowRuleEvent.Type ... events) {
        if (events == null) {
            assertTrue("events generated", listener.events.isEmpty());
        }

        int i = 0;
        for (FlowRuleEvent e : listener.events) {
            assertTrue("unexpected event", e.type().equals(events[i]));
            i++;
        }

        assertEquals("mispredicted number of events",
                events.length, listener.events.size());

        listener.events.clear();
    }

    private int flowCount() {
        return Sets.newHashSet(service.getFlowEntries(DID)).size();
    }
    @Test
    public void getFlowEntries() {
        assertTrue("store should be empty",
                Sets.newHashSet(service.getFlowEntries(DID)).isEmpty());
        addFlowRule(1);
        addFlowRule(2);
        assertEquals("2 rules should exist", 2, flowCount());
        validateEvents(RULE_ADDED, RULE_ADDED);

        addFlowRule(1);
        assertEquals("should still be 2 rules", 2, flowCount());
        validateEvents(RULE_UPDATED);
    }


    //backing store is sensitive to the order of additions/removals
    private boolean validateState(FlowRuleState... state) {
        Iterable<FlowRule> rules = service.getFlowEntries(DID);
        int i = 0;
        for (FlowRule f : rules) {
            if (f.state() != state[i]) {
                return false;
            }
            i++;
        }
        return true;
    }

    @Test
    public void applyFlowRules() {

        FlowRule r1 = flowRule(1, 1);
        FlowRule r2 = flowRule(2, 2);
        FlowRule r3 = flowRule(3, 3);

        assertTrue("store should be empty",
                Sets.newHashSet(service.getFlowEntries(DID)).isEmpty());
        mgr.applyFlowRules(r1, r2, r3);
        assertEquals("3 rules should exist", 3, flowCount());
        assertTrue("Entries should be pending add.",
                validateState(FlowRuleState.PENDING_ADD, FlowRuleState.PENDING_ADD,
                        FlowRuleState.PENDING_ADD));
    }

    @Test
    public void removeFlowRules() {
        FlowRule f1 = addFlowRule(1);
        FlowRule f2 = addFlowRule(2);
        addFlowRule(3);
        assertEquals("3 rules should exist", 3, flowCount());
        validateEvents(RULE_ADDED, RULE_ADDED, RULE_ADDED);

        FlowRule rem1 = flowRule(f1, FlowRuleState.REMOVED);
        FlowRule rem2 = flowRule(f2, FlowRuleState.REMOVED);
        mgr.removeFlowRules(rem1, rem2);
        //removing from north, so no events generated
        validateEvents();
        assertEquals("3 rule should exist", 3, flowCount());
        assertTrue("Entries should be pending remove.",
                validateState(FlowRuleState.CREATED, FlowRuleState.PENDING_REMOVE,
                        FlowRuleState.PENDING_REMOVE));

        mgr.removeFlowRules(rem1);
        assertEquals("3 rule should still exist", 3, flowCount());
    }

    @Test
    public void flowRemoved() {
        FlowRule f1 = addFlowRule(1);
        service.removeFlowRules(f1);
        addFlowRule(2);
        FlowRule rem1 = flowRule(f1, FlowRuleState.REMOVED);
        providerService.flowRemoved(rem1);
        validateEvents(RULE_ADDED, RULE_ADDED, RULE_REMOVED);

        providerService.flowRemoved(rem1);
        validateEvents();
    }

    @Test
    public void flowMetrics() {
        FlowRule f1 = flowRule(1, 1);
        FlowRule f2 = flowRule(2, 2);
        FlowRule f3 = flowRule(3, 3);

        FlowRule updatedF1 = flowRule(f1, FlowRuleState.ADDED);
        FlowRule updatedF2 = flowRule(f2, FlowRuleState.ADDED);
        mgr.applyFlowRules(f1, f2, f3);

        providerService.pushFlowMetrics(DID, Lists.newArrayList(updatedF1, updatedF2));

        assertTrue("Entries should be added.",
                validateState(FlowRuleState.PENDING_ADD, FlowRuleState.ADDED,
                        FlowRuleState.ADDED));
        //TODO: add tests for flowmissing and extraneous flows
    }

    private static class TestListener implements FlowRuleListener {
        final List<FlowRuleEvent> events = new ArrayList<>();

        @Override
        public void event(FlowRuleEvent event) {
            events.add(event);
        }
    }

    private static class TestDeviceService implements DeviceService {

        @Override
        public int getDeviceCount() {
            return 0;
        }

        @Override
        public Iterable<Device> getDevices() {
            return null;
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            return DEV;
        }

        @Override
        public MastershipRole getRole(DeviceId deviceId) {
            return null;
        }

        @Override
        public List<Port> getPorts(DeviceId deviceId) {
            return null;
        }

        @Override
        public Port getPort(DeviceId deviceId, PortNumber portNumber) {
            return null;
        }

        @Override
        public boolean isAvailable(DeviceId deviceId) {
            return false;
        }

        @Override
        public void addListener(DeviceListener listener) {
        }

        @Override
        public void removeListener(DeviceListener listener) {
        }

    }

    private class TestProvider extends AbstractProvider implements FlowRuleProvider {

        protected TestProvider(ProviderId id) {
            super(PID);
        }

        @Override
        public void applyFlowRule(FlowRule... flowRules) {
        }

        @Override
        public void removeFlowRule(FlowRule... flowRules) {
        }

        @Override
        public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        }


    }

    private class TestSelector implements TrafficSelector {

        //for controlling hashcode uniqueness;
        private final int testval;

        public TestSelector(int val) {
            testval = val;
        }

        @Override
        public List<Criterion> criteria() {
            return null;
        }

        @Override
        public int hashCode() {
            return testval;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TestSelector) {
                return this.testval == ((TestSelector) o).testval;
            }
            return false;
        }
    }

    private class TestTreatment implements TrafficTreatment {

        //for controlling hashcode uniqueness;
        private final int testval;

        public TestTreatment(int val) {
            testval = val;
        }

        @Override
        public List<Instruction> instructions() {
            return null;
        }

        @Override
        public int hashCode() {
            return testval;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof TestTreatment) {
                return this.testval == ((TestTreatment) o).testval;
            }
            return false;
        }

    }

}
