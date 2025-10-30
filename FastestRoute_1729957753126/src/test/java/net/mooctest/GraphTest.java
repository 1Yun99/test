package net.mooctest;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class GraphTest {

    private Node node(int id, boolean obstacle, String roadType, boolean tollRoad, boolean restricted,
                      boolean highRisk, double costPerKm, int openTime, int closeTime) {
        return new Node(id, obstacle, roadType, tollRoad, restricted, highRisk, costPerKm, openTime, closeTime);
    }

    // 测试节点的邻接管理与道路开放时间逻辑
    @Test
    public void testNodeAddNeighborAndOpenStatus() {
        Node origin = node(1, false, "Highway", false, false, false, 1.5, 0, 24);
        Node available = node(2, false, "Regular Road", false, false, false, 1.2, 6, 18);
        Node obstacle = node(3, true, "Regular Road", false, false, false, 1.0, 0, 24);

        origin.addNeighbor(available, 10.0);
        origin.addNeighbor(obstacle, 5.0);

        assertEquals(1, origin.getNeighbors().size());
        assertSame(available, origin.getNeighbors().get(0).getNeighbor());
        assertTrue(available.isOpenAt(10));
        assertFalse(available.isOpenAt(4));
        assertEquals("Highway", origin.getRoadType());
        assertFalse(origin.isHighRiskArea());
    }

    // 测试节点属性读取和障碍过滤逻辑
    @Test
    public void testNodeAttributesAndObstacleFiltering() {
        Node tollNode = node(10, false, "Toll Road", true, true, true, 2.5, 8, 20);
        Node blocked = node(11, true, "Regular Road", false, false, false, 1.0, 0, 24);
        tollNode.addNeighbor(blocked, 6.0);

        assertEquals(0, tollNode.getNeighbors().size());
        assertTrue(tollNode.isTollRoad());
        assertTrue(tollNode.isRestrictedForHeavyVehicles());
        assertTrue(tollNode.isHighRiskArea());
        assertEquals(2.5, tollNode.getCostPerKm(), 1e-6);
        assertTrue(tollNode.isOpenAt(10));
        assertFalse(tollNode.isOpenAt(22));
    }

    // 测试节点开放时间边界条件
    @Test
    public void testNodeOpenTimeBoundary() {
        Node boundary = node(20, false, "Regular Road", false, false, false, 1.0, 6, 18);
        assertTrue(boundary.isOpenAt(6));
        assertTrue(boundary.isOpenAt(18));
        assertFalse(boundary.isOpenAt(5));
        assertFalse(boundary.isOpenAt(19));
    }

    // 测试边与路径节点的数据封装可靠性
    @Test
    public void testEdgeAndPathNodeConstructors() {
        Node target = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Edge edge = new Edge(target, 3.5);
        assertSame(target, edge.getNeighbor());
        assertEquals(3.5, edge.getDistance(), 1e-6);

        PathNode pathNode = new PathNode(target, 5.0);
        assertEquals(5.0, pathNode.getDistance(), 1e-6);
        assertEquals(5.0, pathNode.getEstimatedTotalDistance(), 1e-6);

        PathNode pathNode2 = new PathNode(target, 2.0, 4.0);
        assertEquals(2.0, pathNode2.getDistance(), 1e-6);
        assertEquals(4.0, pathNode2.getEstimatedTotalDistance(), 1e-6);
    }

    // 测试图结构的节点注册与边添加能力
    @Test
    public void testGraphAddNodeAndEdge() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);

        graph.addNode(node1);
        graph.addNode(node2);

        graph.addEdge(1, 2, 7.0);
        assertEquals(1, node1.getNeighbors().size());
        assertSame(node2, node1.getNeighbors().get(0).getNeighbor());
        assertEquals(7.0, node1.getNeighbors().get(0).getDistance(), 1e-6);

        graph.addEdge(1, 99, 3.0);
        assertEquals(1, node1.getNeighbors().size());
        assertNull(graph.getNode(99));
    }

    // 测试图在起点缺失时不应创建边
    @Test
    public void testGraphAddEdgeWithMissingFromNode() {
        Graph graph = new Graph();
        Node destination = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(destination);

        graph.addEdge(99, 2, 4.0);
        assertTrue(destination.getNeighbors().isEmpty());
        assertNull(graph.getNode(99));
        assertEquals(1, graph.getNodes().size());
    }

    // 测试图对象提供节点视图的完整性
    @Test
    public void testGraphNodeRegistryExposure() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);

        assertSame(node1, graph.getNode(1));
        assertSame(node2, graph.getNodes().get(2));
        assertEquals(2, graph.getNodes().size());
    }

    // 测试交通状况对路径权重的动态调整
    @Test
    public void testTrafficConditionAdjustments() {
        Map<Integer, String> trafficMap = new HashMap<>();
        trafficMap.put(1, "Congested");
        TrafficCondition trafficCondition = new TrafficCondition(trafficMap);

        assertEquals("Congested", trafficCondition.getTrafficStatus(1));
        assertEquals("Clear", trafficCondition.getTrafficStatus(9));

        assertEquals(10.0, trafficCondition.adjustWeight(5.0, 1), 1e-6);

        trafficCondition.updateTrafficStatus(2, "Closed");
        assertEquals(Double.MAX_VALUE, trafficCondition.adjustWeight(5.0, 2), 0.0);

        trafficCondition.updateTrafficStatus(2, "Accident");
        assertEquals(15.0, trafficCondition.adjustWeight(5.0, 2), 1e-6);

        trafficCondition.updateTrafficStatus(1, "Clear");
        assertEquals(5.0, trafficCondition.adjustWeight(5.0, 1), 1e-6);
    }

    // 测试交通状况的默认分支保持原始权重
    @Test
    public void testTrafficConditionUnknownStatusKeepsWeight() {
        Map<Integer, String> trafficMap = new HashMap<>();
        trafficMap.put(1, "Moderate");
        TrafficCondition trafficCondition = new TrafficCondition(trafficMap);

        assertEquals(8.0, trafficCondition.adjustWeight(8.0, 1), 1e-6);
        trafficCondition.updateTrafficStatus(1, "Clear");
        assertEquals("Clear", trafficCondition.getTrafficStatus(1));
    }

    // 测试天气因素对权重的影响分支
    @Test
    public void testWeatherConditionAdjustments() {
        Node sample = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        assertEquals(15.0, new WeatherCondition("Rainy").adjustWeightForWeather(10.0, sample), 1e-6);
        assertEquals(20.0, new WeatherCondition("Snowy").adjustWeightForWeather(10.0, sample), 1e-6);
        assertEquals(30.0, new WeatherCondition("Stormy").adjustWeightForWeather(10.0, sample), 1e-6);
        assertEquals(10.0, new WeatherCondition("Clear").adjustWeightForWeather(10.0, sample), 1e-6);
    }

    // 测试车辆燃料消耗与加油逻辑
    @Test
    public void testVehicleFuelOperations() {
        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, true, 60.0, 30.0, 2.0, 5.0, false);
        assertEquals("Standard Vehicle", vehicle.getVehicleType());
        assertTrue(vehicle.requiresTollFreeRoute());
        assertEquals(1000.0, vehicle.getMaxLoad(), 1e-6);
        assertFalse(vehicle.isEmergencyVehicle());

        assertFalse(vehicle.needsRefueling(10.0));
        vehicle.consumeFuel(12.0);
        assertTrue(vehicle.needsRefueling(1.0));
        vehicle.refuel(100.0);
        assertEquals(60.0, vehicle.getCurrentFuel(), 1e-6);
        assertFalse(vehicle.needsRefueling(27.5));
    }

    // 测试车辆在燃料恰好等于阈值时无需加油
    @Test
    public void testVehicleNeedsRefuelingAtThreshold() {
        Vehicle vehicle = new Vehicle("Standard Vehicle", 500, false, 60.0, 10.0, 1.0, 4.0, false);
        assertFalse(vehicle.needsRefueling(6.0));
        assertTrue(vehicle.needsRefueling(6.1));
    }

    // 测试加油站加油与费用输出
    @Test
    public void testGasStationRefuelBehavior() {
        Vehicle vehicle = new Vehicle("Standard Vehicle", 500, false, 40.0, 10.0, 1.0, 2.0, false);
        GasStation gasStation = new GasStation(3, 5.5);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        try {
            gasStation.refuel(vehicle, 20.0);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(30.0, vehicle.getCurrentFuel(), 1e-6);
        String output = outputStream.toString();
        assertTrue(output.contains("Refueled 20.0 litres"));
        assertTrue(output.contains("node 3"));
    }

    // 测试加油站输出费用详情
    @Test
    public void testGasStationRefuelCostMessage() {
        Vehicle vehicle = new Vehicle("Standard Vehicle", 800, false, 70.0, 25.0, 1.0, 5.0, false);
        GasStation gasStation = new GasStation(9, 5.5);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        try {
            gasStation.refuel(vehicle, 15.0);
        } finally {
            System.setOut(originalOut);
        }

        assertEquals(40.0, vehicle.getCurrentFuel(), 1e-6);
        String message = outputStream.toString();
        assertTrue(message.contains("Refueled 15.0 litres"));
        assertTrue(message.contains("node 9"));
        assertTrue(message.contains("82.5"));
    }

    // 测试路径优化器对搜索算法的委托
    @Test
    public void testRouteOptimizerDelegation() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node end = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(end);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 10.0, 10.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");
        final boolean[] called = {false};

        SearchAlgorithm algorithm = new SearchAlgorithm(graph, start, end, vehicle, trafficCondition, weatherCondition, 0) {
            @Override
            public PathResult findPath() {
                called[0] = true;
                return new PathResult(Arrays.asList(start, end));
            }
        };

        RouteOptimizer optimizer = new RouteOptimizer(algorithm);
        PathResult result = optimizer.optimizeRoute();
        assertTrue(called[0]);
        assertEquals(Arrays.asList(start, end), result.getPath());
    }

    // 测试Dijkstra算法在复杂约束下的路径搜索
    @Test
    public void testDijkstraHandlesConstraintsAndGasStations() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node3 = node(3, false, "Regular Road", false, false, true, 1.0, 0, 24);
        Node node4 = node(4, false, "Regular Road", false, false, false, 1.0, 5, 10);
        Node node5 = node(5, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node6 = node(6, false, "Regular Road", false, false, false, 1.0, 0, 24);

        graph.addNode(start);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(node4);
        graph.addNode(node5);
        graph.addNode(node6);

        graph.addEdge(1, 2, 5.0);
        graph.addEdge(1, 3, 1.0);
        graph.addEdge(1, 4, 2.0);
        graph.addEdge(2, 5, 10.0);
        graph.addEdge(2, 6, 80.0);

        Map<Integer, String> trafficMap = new HashMap<>();
        trafficMap.put(2, "Congested");
        trafficMap.put(4, "Closed");
        trafficMap.put(6, "Accident");
        TrafficCondition trafficCondition = new TrafficCondition(trafficMap);
        WeatherCondition weatherCondition = new WeatherCondition("Rainy");

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 100.0, 5.0, 2.0, 1.0, false);
        Map<Integer, GasStation> gasStations = new HashMap<>();
        gasStations.put(1, new GasStation(1, 2.5));

        Dijkstra dijkstra = new Dijkstra(graph, start, node5, vehicle, trafficCondition, weatherCondition, 0, gasStations);
        PathResult result = dijkstra.findPath();

        assertNotNull(result);
        assertEquals(Arrays.asList(start, node2, node5), result.getPath());
        assertEquals(100.0, vehicle.getCurrentFuel(), 1e-6);
        assertFalse(result.getPath().contains(node3));
        assertFalse(result.getPath().contains(node4));
    }

    // 测试应急车辆在Dijkstra算法中的优先通行
    @Test
    public void testDijkstraAllowsEmergencyThroughRestrictions() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node risky = node(2, false, "Regular Road", false, false, true, 1.0, 10, 12);
        graph.addNode(start);
        graph.addNode(risky);
        graph.addEdge(1, 2, 5.0);

        Vehicle emergencyVehicle = new Vehicle("Ambulance", 1000, false, 100.0, 50.0, 1.0, 0.0, true);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");
        Map<Integer, GasStation> gasStations = new HashMap<>();

        Dijkstra dijkstra = new Dijkstra(graph, start, risky, emergencyVehicle, trafficCondition, weatherCondition, 0, gasStations);
        PathResult result = dijkstra.findPath();

        assertNotNull(result);
        assertEquals(Arrays.asList(start, risky), result.getPath());
    }

    // 测试Dijkstra算法在缺乏加油站时的失败分支
    @Test
    public void testDijkstraFailsWhenFuelInsufficientAndNoStation() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node end = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(end);
        graph.addEdge(1, 2, 5.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 20.0, 2.0, 2.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        Dijkstra dijkstra = new Dijkstra(graph, start, end, vehicle, trafficCondition, weatherCondition, 0, new HashMap<Integer, GasStation>());
        assertNull(dijkstra.findPath());
    }

    // 测试Dijkstra路径重建工具方法
    @Test
    public void testDijkstraReconstructPathUtility() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node mid = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node end = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(mid);
        graph.addNode(end);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 500, false, 50.0, 20.0, 1.0, 0.0, false);
        TrafficCondition traffic = new TrafficCondition(new HashMap<>());
        WeatherCondition weather = new WeatherCondition("Clear");

        Dijkstra dijkstra = new Dijkstra(graph, start, end, vehicle, traffic, weather, 0, new HashMap<Integer, GasStation>());
        Map<Node, Node> predecessors = new HashMap<>();
        predecessors.put(end, mid);
        predecessors.put(mid, start);
        PathResult result = dijkstra.reconstructPath(predecessors);
        assertEquals(Arrays.asList(start, mid, end), result.getPath());
    }

    // 测试Dijkstra算法起点即终点时的返回结果
    @Test
    public void testDijkstraWhenAlreadyAtDestination() {
        Graph graph = new Graph();
        Node single = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(single);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 200, false, 30.0, 30.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        Dijkstra dijkstra = new Dijkstra(graph, single, single, vehicle, trafficCondition, weatherCondition, 0, new HashMap<Integer, GasStation>());
        PathResult result = dijkstra.findPath();
        assertNotNull(result);
        assertEquals(Collections.singletonList(single), result.getPath());
    }

    // 测试Dijkstra算法在发现更短路径时会更新距离
    @Test
    public void testDijkstraUpdatesShorterDistance() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node midPriority = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node midAlternative = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node end = node(4, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(midPriority);
        graph.addNode(midAlternative);
        graph.addNode(end);

        graph.addEdge(1, 2, 5.0);
        graph.addEdge(1, 3, 1.0);
        graph.addEdge(3, 2, 1.0);
        graph.addEdge(2, 4, 1.0);
        graph.addEdge(3, 4, 10.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 80.0, 80.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        Dijkstra dijkstra = new Dijkstra(graph, start, end, vehicle, trafficCondition, weatherCondition, 0, new HashMap<Integer, GasStation>());
        PathResult result = dijkstra.findPath();

        assertNotNull(result);
        assertEquals(Arrays.asList(start, midAlternative, midPriority, end), result.getPath());
    }

    // 测试A*算法的启发式函数与过滤路径逻辑
    @Test
    public void testAStarHeuristicAndPathSelection() {
        Graph graph = new Graph();
        Node start = node(1, false, "Highway", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node3 = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node risky = node(4, false, "Highway", false, false, true, 1.0, 0, 24);
        Node end = node(5, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node closed = node(6, false, "Regular Road", false, false, false, 1.0, 5, 10);

        graph.addNode(start);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addNode(risky);
        graph.addNode(end);
        graph.addNode(closed);

        graph.addEdge(1, 2, 2.0);
        graph.addEdge(1, 3, 3.0);
        graph.addEdge(1, 4, 1.0);
        graph.addEdge(1, 6, 1.0);
        graph.addEdge(2, 5, 3.0);
        graph.addEdge(3, 5, 1.0);

        Map<Integer, String> trafficMap = new HashMap<>();
        trafficMap.put(3, "Accident");
        trafficMap.put(6, "Congested");
        TrafficCondition trafficCondition = new TrafficCondition(trafficMap);
        WeatherCondition weatherCondition = new WeatherCondition("Snowy");
        Vehicle vehicle = new Vehicle("Standard Vehicle", 500, false, 20.0, 10.0, 1.0, 0.0, false);

        AStar aStar = new AStar(graph, start, end, vehicle, trafficCondition, weatherCondition, 0);
        assertEquals(3.2, aStar.heuristic(start), 1e-6);
        assertEquals(2.0, aStar.heuristic(risky), 1e-6);

        PathResult result = aStar.findPath();
        assertNotNull(result);
        assertEquals(Arrays.asList(start, node2, end), result.getPath());
        assertFalse(result.getPath().contains(risky));
    }

    // 测试应急车辆在A*算法中忽略限制
    @Test
    public void testAStarEmergencyVehicleIgnoresRestrictions() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node risky = node(2, false, "Highway", false, false, true, 1.0, 5, 10);
        Node end = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(risky);
        graph.addNode(end);

        graph.addEdge(1, 2, 1.0);
        graph.addEdge(2, 3, 1.0);

        Vehicle emergencyVehicle = new Vehicle("Ambulance", 1000, false, 50.0, 50.0, 1.0, 0.0, true);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        AStar aStar = new AStar(graph, start, end, emergencyVehicle, trafficCondition, weatherCondition, 0);
        PathResult result = aStar.findPath();
        assertNotNull(result);
        assertEquals(Arrays.asList(start, risky, end), result.getPath());
    }

    // 测试A*算法在燃料不足时跳过邻居节点
    @Test
    public void testAStarSkipsNeighborDueToFuelConstraint() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node end = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(end);
        graph.addEdge(1, 2, 1.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 500, false, 10.0, 1.0, 1.0, 0.5, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        AStar aStar = new AStar(graph, start, end, vehicle, trafficCondition, weatherCondition, 0);
        assertNull(aStar.findPath());
    }

    // 测试A*算法在路径受限时返回空结果
    @Test
    public void testAStarReturnsNullWhenAllPathsFiltered() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node blocked = node(2, false, "Regular Road", false, false, false, 1.0, 10, 12);
        graph.addNode(start);
        graph.addNode(blocked);
        graph.addEdge(1, 2, 1.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 500, false, 20.0, 10.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        AStar aStar = new AStar(graph, start, blocked, vehicle, trafficCondition, weatherCondition, 0);
        assertNull(aStar.findPath());
    }

    // 测试Bellman-Ford算法的最短路径输出
    @Test
    public void testBellmanFordFindsShortestPath() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node3 = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);

        graph.addEdge(1, 2, 2.0);
        graph.addEdge(2, 3, 2.0);
        graph.addEdge(1, 3, 10.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 500, false, 20.0, 10.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        BellmanFord bellmanFord = new BellmanFord(graph, node1, node3, vehicle, trafficCondition, weatherCondition, 0);
        PathResult result = bellmanFord.findPath();
        assertNotNull(result);
        assertEquals(Arrays.asList(node1, node2, node3), result.getPath());
    }

    // 测试Bellman-Ford算法的负环检测能力
    @Test
    public void testBellmanFordDetectsNegativeCycle() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);

        graph.addEdge(1, 2, -5.0);
        graph.addEdge(2, 1, -1.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 500, false, 20.0, 10.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        BellmanFord bellmanFord = new BellmanFord(graph, node1, node2, vehicle, trafficCondition, weatherCondition, 0);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        PathResult result;
        try {
            result = bellmanFord.findPath();
        } finally {
            System.setOut(originalOut);
        }

        assertNull(result);
        assertTrue(outputStream.toString().contains("Graph contains negative weight cycle"));
    }

    // 测试Floyd-Warshall算法的路径恢复与矩阵输出
    @Test
    public void testFloydWarshallShortestPathAndMatrixOutput() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node3 = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);

        graph.addEdge(1, 2, 3.0);
        graph.addEdge(2, 3, 4.0);
        graph.addEdge(1, 3, 10.0);

        FloydWarshall floydWarshall = new FloydWarshall(graph);
        List<Node> path = floydWarshall.getShortestPath(node1, node3);
        assertEquals(Arrays.asList(node1, node2, node3), path);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        try {
            floydWarshall.printDistanceMatrix();
        } finally {
            System.setOut(originalOut);
        }
        String output = outputStream.toString();
        assertTrue(output.contains("7.0"));
        assertTrue(output.contains("∞"));
    }

    // 测试Floyd-Warshall算法在无路径时的返回值
    @Test
    public void testFloydWarshallNoPathReturnsEmpty() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node3 = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);

        graph.addEdge(1, 2, 3.0);

        FloydWarshall floydWarshall = new FloydWarshall(graph);
        List<Node> path = floydWarshall.getShortestPath(node2, node3);
        assertTrue(path.isEmpty());
    }

    // 测试ShortestTimeFirst算法的行驶时间计算
    @Test
    public void testShortestTimeFirstCalculateTravelTime() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node end = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(end);

        Vehicle standardVehicle = new Vehicle("Standard Vehicle", 1000, false, 50.0, 50.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");
        ShortestTimeFirst shortestTimeFirst = new ShortestTimeFirst(graph, start, end, standardVehicle, trafficCondition, weatherCondition, 0);

        Node highwayNode = node(3, false, "Highway", false, false, false, 1.0, 0, 24);
        Node tollNode = node(4, false, "Toll Road", false, false, false, 1.0, 0, 24);
        Node regularNode = node(5, false, "Regular Road", false, false, false, 1.0, 0, 24);

        Edge highwayEdge = new Edge(highwayNode, 150.0);
        Edge tollEdge = new Edge(tollNode, 80.0);
        Edge regularEdge = new Edge(regularNode, 50.0);

        assertEquals(1.5, shortestTimeFirst.calculateTravelTime(highwayEdge, standardVehicle), 1e-6);
        assertEquals(1.0, shortestTimeFirst.calculateTravelTime(tollEdge, standardVehicle), 1e-6);
        assertEquals(1.0, shortestTimeFirst.calculateTravelTime(regularEdge, standardVehicle), 1e-6);

        Vehicle heavyVehicle = new Vehicle("Heavy Vehicle", 1000, false, 50.0, 50.0, 1.0, 0.0, false);
        Edge heavyEdge = new Edge(highwayNode, 75.0);
        assertEquals(1.0, shortestTimeFirst.calculateTravelTime(heavyEdge, heavyVehicle), 1e-6);
    }

    // 测试ShortestTimeFirst算法在道路封闭时的路径选择
    @Test
    public void testShortestTimeFirstFindPathWithClosures() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Highway", false, false, false, 1.0, 0, 24);
        Node end = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node closed = node(4, false, "Regular Road", false, false, false, 1.0, 5, 10);

        graph.addNode(start);
        graph.addNode(node2);
        graph.addNode(end);
        graph.addNode(closed);

        graph.addEdge(1, 2, 100.0);
        graph.addEdge(1, 4, 10.0);
        graph.addEdge(2, 3, 50.0);

        Map<Integer, String> trafficMap = new HashMap<>();
        trafficMap.put(2, "Congested");
        TrafficCondition trafficCondition = new TrafficCondition(trafficMap);
        WeatherCondition weatherCondition = new WeatherCondition("Rainy");
        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 100.0, 50.0, 1.0, 0.0, false);

        ShortestTimeFirst shortestTimeFirst = new ShortestTimeFirst(graph, start, end, vehicle, trafficCondition, weatherCondition, 0);
        PathResult result = shortestTimeFirst.findPath();
        assertNotNull(result);
        assertEquals(Arrays.asList(start, node2, end), result.getPath());
    }

    // 测试ShortestTimeFirst算法能跳过关闭道路
    @Test
    public void testShortestTimeFirstSkipsClosedNeighbor() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node closed = node(2, false, "Regular Road", false, false, false, 1.0, 0, 5);
        Node end = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(closed);
        graph.addNode(end);

        graph.addEdge(1, 2, 600.0);
        graph.addEdge(2, 3, 10.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 800, false, 100.0, 50.0, 1.0, 0.0, false);
        Map<Integer, String> trafficMap = new HashMap<>();
        TrafficCondition trafficCondition = new TrafficCondition(trafficMap);
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        ShortestTimeFirst shortestTimeFirst = new ShortestTimeFirst(graph, start, end, vehicle, trafficCondition, weatherCondition, 0);
        assertNull(shortestTimeFirst.findPath());
    }

    // 测试ShortestTimeFirst算法的路径重建方法
    @Test
    public void testShortestTimeFirstReconstructPathUtility() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node mid = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node end = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(start);
        graph.addNode(mid);
        graph.addNode(end);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 100.0, 50.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        ShortestTimeFirst shortestTimeFirst = new ShortestTimeFirst(graph, start, end, vehicle, trafficCondition, weatherCondition, 0);
        Map<Node, Node> predecessors = new HashMap<>();
        predecessors.put(end, mid);
        predecessors.put(mid, start);
        PathResult result = shortestTimeFirst.reconstructPath(predecessors);
        assertEquals(Arrays.asList(start, mid, end), result.getPath());
    }

    // 测试ShortestTimeFirst算法在全部路径受限时的返回
    @Test
    public void testShortestTimeFirstReturnsNullWhenBlocked() {
        Graph graph = new Graph();
        Node start = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node blocked = node(2, false, "Regular Road", false, false, false, 1.0, 10, 12);
        graph.addNode(start);
        graph.addNode(blocked);
        graph.addEdge(1, 2, 50.0);

        Map<Integer, String> trafficMap = new HashMap<>();
        trafficMap.put(2, "Congested");
        TrafficCondition trafficCondition = new TrafficCondition(trafficMap);
        WeatherCondition weatherCondition = new WeatherCondition("Stormy");
        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 100.0, 50.0, 1.0, 0.0, false);

        ShortestTimeFirst shortestTimeFirst = new ShortestTimeFirst(graph, start, blocked, vehicle, trafficCondition, weatherCondition, 0);
        assertNull(shortestTimeFirst.findPath());
    }

    // 测试迭代加深搜索在深度限制内成功找路
    @Test
    public void testIterativeDeepeningSearchFindsPathWithinDepth() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node3 = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addEdge(1, 2, 1.0);
        graph.addEdge(2, 3, 1.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 100.0, 50.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        IterativeDeepeningSearch ids = new IterativeDeepeningSearch(graph, node1, node3, vehicle, trafficCondition, weatherCondition, 0, 3);
        PathResult result = ids.findPath();
        assertNotNull(result);
        assertEquals(Arrays.asList(node1, node2, node3), result.getPath());
    }

    // 测试迭代加深搜索的深度剪枝与基础情况
    @Test
    public void testIterativeDeepeningSearchDepthControl() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node3 = node(3, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);
        graph.addNode(node3);
        graph.addEdge(1, 2, 1.0);
        graph.addEdge(2, 3, 1.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 100.0, 50.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        IterativeDeepeningSearch ids = new IterativeDeepeningSearch(graph, node1, node3, vehicle, trafficCondition, weatherCondition, 0, 2);
        Set<Node> visited = new HashSet<>();
        PathResult limitedResult = ids.depthLimitedSearch(node1, node3, 1, visited);
        assertNull(limitedResult);

        PathResult zeroDepthResult = ids.depthLimitedSearch(node3, node3, 0, new HashSet<Node>());
        assertNotNull(zeroDepthResult);
        assertEquals(Collections.singletonList(node3), zeroDepthResult.getPath());
    }

    // 测试迭代加深搜索在已访问集合的剪枝行为
    @Test
    public void testIterativeDeepeningSearchVisitedPreventsRevisit() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);
        node1.addNeighbor(node2, 1.0);
        node2.addNeighbor(node1, 1.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 100.0, 50.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        IterativeDeepeningSearch ids = new IterativeDeepeningSearch(graph, node1, node2, vehicle, trafficCondition, weatherCondition, 0, 2);
        Set<Node> visited = new HashSet<>();
        visited.add(node2);
        PathResult result = ids.depthLimitedSearch(node1, node2, 1, visited);
        assertNull(result);
        assertFalse(visited.contains(node1));
        assertTrue(visited.contains(node2));
    }

    // 测试迭代加深搜索在深度为零且节点不同时时返回空
    @Test
    public void testIterativeDeepeningSearchDepthZeroNoMatch() {
        Graph graph = new Graph();
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        graph.addNode(node1);
        graph.addNode(node2);
        node1.addNeighbor(node2, 1.0);

        Vehicle vehicle = new Vehicle("Standard Vehicle", 1000, false, 100.0, 50.0, 1.0, 0.0, false);
        TrafficCondition trafficCondition = new TrafficCondition(new HashMap<>());
        WeatherCondition weatherCondition = new WeatherCondition("Clear");

        IterativeDeepeningSearch ids = new IterativeDeepeningSearch(graph, node1, node2, vehicle, trafficCondition, weatherCondition, 0, 1);
        PathResult result = ids.depthLimitedSearch(node1, node2, 0, new HashSet<Node>());
        assertNull(result);
    }

    // 测试路径结果对象的打印输出
    @Test
    public void testPathResultPrintsCorrectly() {
        Node node1 = node(1, false, "Regular Road", false, false, false, 1.0, 0, 24);
        Node node2 = node(2, false, "Regular Road", false, false, false, 1.0, 0, 24);
        List<Node> path = new ArrayList<>();
        path.add(node1);
        path.add(node2);
        PathResult pathResult = new PathResult(path);
        assertSame(path, pathResult.getPath());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
        try {
            pathResult.printPath();
        } finally {
            System.setOut(originalOut);
        }

        assertEquals("1 -> 2 -> End\n", outputStream.toString());
    }
}
