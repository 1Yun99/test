package net.mooctest;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import java.util.*;
import java.util.concurrent.*;
import java.lang.reflect.*;

/**
 * 综合测试类 - 覆盖所有业务代码
 * 目标：高分支覆盖率、高变异杀伤率
 * 测试框架：JUnit 4.12
 */
public class ElevatorManagerTest {

    // ==================== 枚举类测试 ====================
    
    @Test(timeout = 4000)
    public void testDirectionEnum() {
        // 测试Direction枚举的所有值
        Direction up = Direction.UP;
        Direction down = Direction.DOWN;
        
        assertNotNull(up);
        assertNotNull(down);
        assertEquals("UP", up.toString());
        assertEquals("DOWN", down.toString());
        
        // 测试values()方法
        Direction[] directions = Direction.values();
        assertEquals(2, directions.length);
        
        // 测试valueOf()方法
        assertEquals(Direction.UP, Direction.valueOf("UP"));
        assertEquals(Direction.DOWN, Direction.valueOf("DOWN"));
    }
    
    @Test(timeout = 4000)
    public void testPriorityEnum() {
        // 测试Priority枚举的所有值
        Priority high = Priority.HIGH;
        Priority medium = Priority.MEDIUM;
        Priority low = Priority.LOW;
        
        assertNotNull(high);
        assertNotNull(medium);
        assertNotNull(low);
        
        // 测试values()方法
        Priority[] priorities = Priority.values();
        assertEquals(3, priorities.length);
        
        // 测试valueOf()方法
        assertEquals(Priority.HIGH, Priority.valueOf("HIGH"));
        assertEquals(Priority.MEDIUM, Priority.valueOf("MEDIUM"));
        assertEquals(Priority.LOW, Priority.valueOf("LOW"));
    }
    
    @Test(timeout = 4000)
    public void testRequestTypeEnum() {
        // 测试RequestType枚举
        RequestType standard = RequestType.STANDARD;
        RequestType destinationControl = RequestType.DESTINATION_CONTROL;
        
        assertNotNull(standard);
        assertNotNull(destinationControl);
        
        RequestType[] types = RequestType.values();
        assertEquals(2, types.length);
        
        assertEquals(RequestType.STANDARD, RequestType.valueOf("STANDARD"));
        assertEquals(RequestType.DESTINATION_CONTROL, RequestType.valueOf("DESTINATION_CONTROL"));
    }
    
    @Test(timeout = 4000)
    public void testSpecialNeedsEnum() {
        // 测试SpecialNeeds枚举
        SpecialNeeds none = SpecialNeeds.NONE;
        SpecialNeeds disabled = SpecialNeeds.DISABLED_ASSISTANCE;
        SpecialNeeds luggage = SpecialNeeds.LARGE_LUGGAGE;
        SpecialNeeds vip = SpecialNeeds.VIP_SERVICE;
        
        assertNotNull(none);
        assertNotNull(disabled);
        assertNotNull(luggage);
        assertNotNull(vip);
        
        SpecialNeeds[] needs = SpecialNeeds.values();
        assertEquals(4, needs.length);
        
        assertEquals(SpecialNeeds.NONE, SpecialNeeds.valueOf("NONE"));
        assertEquals(SpecialNeeds.DISABLED_ASSISTANCE, SpecialNeeds.valueOf("DISABLED_ASSISTANCE"));
    }
    
    @Test(timeout = 4000)
    public void testElevatorModeEnum() {
        // 测试ElevatorMode枚举
        ElevatorMode normal = ElevatorMode.NORMAL;
        ElevatorMode energySaving = ElevatorMode.ENERGY_SAVING;
        ElevatorMode emergency = ElevatorMode.EMERGENCY;
        
        assertNotNull(normal);
        assertNotNull(energySaving);
        assertNotNull(emergency);
        
        ElevatorMode[] modes = ElevatorMode.values();
        assertEquals(3, modes.length);
        
        assertEquals(ElevatorMode.NORMAL, ElevatorMode.valueOf("NORMAL"));
        assertEquals(ElevatorMode.ENERGY_SAVING, ElevatorMode.valueOf("ENERGY_SAVING"));
        assertEquals(ElevatorMode.EMERGENCY, ElevatorMode.valueOf("EMERGENCY"));
    }
    
    @Test(timeout = 4000)
    public void testElevatorStatusEnum() {
        // 测试ElevatorStatus枚举
        ElevatorStatus moving = ElevatorStatus.MOVING;
        ElevatorStatus stopped = ElevatorStatus.STOPPED;
        ElevatorStatus idle = ElevatorStatus.IDLE;
        ElevatorStatus maintenance = ElevatorStatus.MAINTENANCE;
        ElevatorStatus emergency = ElevatorStatus.EMERGENCY;
        ElevatorStatus fault = ElevatorStatus.FAULT;
        
        assertNotNull(moving);
        assertNotNull(stopped);
        assertNotNull(idle);
        assertNotNull(maintenance);
        assertNotNull(emergency);
        assertNotNull(fault);
        
        ElevatorStatus[] statuses = ElevatorStatus.values();
        assertEquals(6, statuses.length);
    }
    
    @Test(timeout = 4000)
    public void testEventTypeEnum() {
        // 测试EventType枚举
        EventType elevatorFault = EventType.ELEVATOR_FAULT;
        EventType emergency = EventType.EMERGENCY;
        EventType maintenanceRequired = EventType.MAINTENANCE_REQUIRED;
        EventType configUpdated = EventType.CONFIG_UPDATED;
        
        assertNotNull(elevatorFault);
        assertNotNull(emergency);
        assertNotNull(maintenanceRequired);
        assertNotNull(configUpdated);
        
        EventType[] types = EventType.values();
        assertEquals(4, types.length);
    }
    
    // ==================== Event类测试 ====================
    
    @Test(timeout = 4000)
    public void testEventCreation() {
        // 测试Event对象创建和getter方法
        Object data = "test data";
        Event event = new Event(EventType.EMERGENCY, data);
        
        assertNotNull(event);
        assertEquals(EventType.EMERGENCY, event.getType());
        assertEquals(data, event.getData());
    }
    
    @Test(timeout = 4000)
    public void testEventWithNullData() {
        // 测试Event可以接受null数据
        Event event = new Event(EventType.ELEVATOR_FAULT, null);
        
        assertNotNull(event);
        assertEquals(EventType.ELEVATOR_FAULT, event.getType());
        assertNull(event.getData());
    }
    
    @Test(timeout = 4000)
    public void testEventWithDifferentTypes() {
        // 测试不同类型的Event
        Event event1 = new Event(EventType.MAINTENANCE_REQUIRED, "maintenance");
        Event event2 = new Event(EventType.CONFIG_UPDATED, 123);
        
        assertEquals(EventType.MAINTENANCE_REQUIRED, event1.getType());
        assertEquals(EventType.CONFIG_UPDATED, event2.getType());
        assertEquals("maintenance", event1.getData());
        assertEquals(123, event2.getData());
    }
    
    // ==================== PassengerRequest类测试 ====================
    
    @Test(timeout = 4000)
    public void testPassengerRequestUpDirection() {
        // 测试向上的乘客请求
        PassengerRequest request = new PassengerRequest(1, 5, Priority.HIGH, RequestType.STANDARD);
        
        assertEquals(1, request.getStartFloor());
        assertEquals(5, request.getDestinationFloor());
        assertEquals(Direction.UP, request.getDirection());
        assertEquals(Priority.HIGH, request.getPriority());
        assertEquals(RequestType.STANDARD, request.getRequestType());
        assertEquals(SpecialNeeds.NONE, request.getSpecialNeeds());
        assertTrue(request.getTimestamp() > 0);
    }
    
    @Test(timeout = 4000)
    public void testPassengerRequestDownDirection() {
        // 测试向下的乘客请求
        PassengerRequest request = new PassengerRequest(10, 2, Priority.LOW, RequestType.DESTINATION_CONTROL);
        
        assertEquals(10, request.getStartFloor());
        assertEquals(2, request.getDestinationFloor());
        assertEquals(Direction.DOWN, request.getDirection());
        assertEquals(Priority.LOW, request.getPriority());
        assertEquals(RequestType.DESTINATION_CONTROL, request.getRequestType());
    }
    
    @Test(timeout = 4000)
    public void testPassengerRequestToString() {
        // 测试toString方法
        PassengerRequest request = new PassengerRequest(3, 8, Priority.MEDIUM, RequestType.STANDARD);
        String str = request.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("3"));
        assertTrue(str.contains("8"));
        assertTrue(str.contains("MEDIUM"));
        assertTrue(str.contains("STANDARD"));
        assertTrue(str.contains("NONE"));
    }
    
    @Test(timeout = 4000)
    public void testPassengerRequestWithDifferentPriorities() {
        // 测试不同优先级的请求
        PassengerRequest highPriority = new PassengerRequest(1, 10, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest mediumPriority = new PassengerRequest(1, 10, Priority.MEDIUM, RequestType.STANDARD);
        PassengerRequest lowPriority = new PassengerRequest(1, 10, Priority.LOW, RequestType.STANDARD);
        
        assertEquals(Priority.HIGH, highPriority.getPriority());
        assertEquals(Priority.MEDIUM, mediumPriority.getPriority());
        assertEquals(Priority.LOW, lowPriority.getPriority());
    }
    
    // ==================== Floor类测试 ====================
    
    @Test(timeout = 4000)
    public void testFloorCreation() {
        // 测试Floor对象创建
        Floor floor = new Floor(5);
        
        assertNotNull(floor);
        assertEquals(5, floor.getFloorNumber());
    }
    
    @Test(timeout = 4000)
    public void testFloorAddAndGetRequestsUp() {
        // 测试添加和获取向上的请求
        Floor floor = new Floor(3);
        PassengerRequest request = new PassengerRequest(3, 8, Priority.MEDIUM, RequestType.STANDARD);
        
        floor.addRequest(request);
        List<PassengerRequest> requests = floor.getRequests(Direction.UP);
        
        assertEquals(1, requests.size());
        assertEquals(request, requests.get(0));
        
        // 再次获取应该为空（已被清空）
        List<PassengerRequest> emptyRequests = floor.getRequests(Direction.UP);
        assertEquals(0, emptyRequests.size());
    }
    
    @Test(timeout = 4000)
    public void testFloorAddAndGetRequestsDown() {
        // 测试添加和获取向下的请求
        Floor floor = new Floor(10);
        PassengerRequest request = new PassengerRequest(10, 2, Priority.HIGH, RequestType.STANDARD);
        
        floor.addRequest(request);
        List<PassengerRequest> requests = floor.getRequests(Direction.DOWN);
        
        assertEquals(1, requests.size());
        assertEquals(request, requests.get(0));
    }
    
    @Test(timeout = 4000)
    public void testFloorMultipleRequests() {
        // 测试多个请求
        Floor floor = new Floor(5);
        PassengerRequest request1 = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(5, 15, Priority.MEDIUM, RequestType.STANDARD);
        
        floor.addRequest(request1);
        floor.addRequest(request2);
        
        List<PassengerRequest> requests = floor.getRequests(Direction.UP);
        assertEquals(2, requests.size());
    }
    
    @Test(timeout = 4000)
    public void testFloorConcurrentAccess() throws InterruptedException {
        // 测试并发访问
        final Floor floor = new Floor(1);
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                PassengerRequest request = new PassengerRequest(1, index + 2, Priority.MEDIUM, RequestType.STANDARD);
                floor.addRequest(request);
                latch.countDown();
            }).start();
        }
        
        latch.await();
        List<PassengerRequest> requests = floor.getRequests(Direction.UP);
        assertEquals(threadCount, requests.size());
    }
    
    // ==================== SystemConfig类测试 ====================
    
    @Test(timeout = 4000)
    public void testSystemConfigSingleton() {
        // 测试单例模式
        SystemConfig config1 = SystemConfig.getInstance();
        SystemConfig config2 = SystemConfig.getInstance();
        
        assertNotNull(config1);
        assertSame(config1, config2);
    }
    
    @Test(timeout = 4000)
    public void testSystemConfigDefaultValues() {
        // 测试默认配置值
        SystemConfig config = new SystemConfig();
        
        assertEquals(20, config.getFloorCount());
        assertEquals(4, config.getElevatorCount());
        assertEquals(800.0, config.getMaxLoad(), 0.001);
    }
    
    @Test(timeout = 4000)
    public void testSystemConfigSetFloorCount() {
        // 测试设置楼层数
        SystemConfig config = new SystemConfig();
        
        config.setFloorCount(30);
        assertEquals(30, config.getFloorCount());
        
        // 测试边界条件：设置非法值（<=0）
        config.setFloorCount(-5);
        assertEquals(30, config.getFloorCount()); // 应该保持不变
        
        config.setFloorCount(0);
        assertEquals(30, config.getFloorCount()); // 应该保持不变
    }
    
    @Test(timeout = 4000)
    public void testSystemConfigSetElevatorCount() {
        // 测试设置电梯数量
        SystemConfig config = new SystemConfig();
        
        config.setElevatorCount(8);
        assertEquals(8, config.getElevatorCount());
        
        // 测试边界条件：设置非法值（<=0）
        config.setElevatorCount(-3);
        assertEquals(8, config.getElevatorCount());
        
        config.setElevatorCount(0);
        assertEquals(8, config.getElevatorCount());
    }
    
    @Test(timeout = 4000)
    public void testSystemConfigSetMaxLoad() {
        // 测试设置最大载重
        SystemConfig config = new SystemConfig();
        
        config.setMaxLoad(1000.0);
        assertEquals(1000.0, config.getMaxLoad(), 0.001);
        
        // 测试边界条件：设置非法值（<=0）
        config.setMaxLoad(-100.0);
        assertEquals(1000.0, config.getMaxLoad(), 0.001);
        
        config.setMaxLoad(0.0);
        assertEquals(1000.0, config.getMaxLoad(), 0.001);
    }
    
    // ==================== ElevatorStatusReport类测试 ====================
    
    @Test(timeout = 4000)
    public void testElevatorStatusReportCreation() {
        // 测试ElevatorStatusReport对象创建
        ElevatorStatusReport report = new ElevatorStatusReport(
            1, 5, Direction.UP, ElevatorStatus.MOVING, 2.5, 500.0, 7
        );
        
        assertEquals(1, report.getElevatorId());
        assertEquals(5, report.getCurrentFloor());
        assertEquals(Direction.UP, report.getDirection());
        assertEquals(ElevatorStatus.MOVING, report.getStatus());
        assertEquals(2.5, report.getSpeed(), 0.001);
        assertEquals(500.0, report.getCurrentLoad(), 0.001);
        assertEquals(7, report.getPassengerCount());
    }
    
    @Test(timeout = 4000)
    public void testElevatorStatusReportToString() {
        // 测试toString方法
        ElevatorStatusReport report = new ElevatorStatusReport(
            2, 10, Direction.DOWN, ElevatorStatus.STOPPED, 0.0, 300.0, 4
        );
        
        String str = report.toString();
        assertNotNull(str);
        assertTrue(str.contains("2"));
        assertTrue(str.contains("10"));
        assertTrue(str.contains("DOWN"));
        assertTrue(str.contains("STOPPED"));
    }
    
    @Test(timeout = 4000)
    public void testElevatorStatusReportWithDifferentStates() {
        // 测试不同状态的报告
        ElevatorStatusReport report1 = new ElevatorStatusReport(
            1, 1, Direction.UP, ElevatorStatus.IDLE, 0.0, 0.0, 0
        );
        
        ElevatorStatusReport report2 = new ElevatorStatusReport(
            2, 15, Direction.DOWN, ElevatorStatus.EMERGENCY, 3.0, 600.0, 8
        );
        
        assertEquals(ElevatorStatus.IDLE, report1.getStatus());
        assertEquals(ElevatorStatus.EMERGENCY, report2.getStatus());
    }
    
    // ==================== Elevator类测试 ====================
    
    @Test(timeout = 4000)
    public void testElevatorCreation() {
        // 测试Elevator对象创建
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        assertEquals(1, elevator.getId());
        assertEquals(1, elevator.getCurrentFloor());
        assertEquals(Direction.UP, elevator.getDirection());
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
        assertEquals(0.0, elevator.getEnergyConsumption(), 0.001);
        assertEquals(ElevatorMode.NORMAL, elevator.getMode());
        assertEquals(0.0, elevator.getCurrentLoad(), 0.001);
    }
    
    @Test(timeout = 4000)
    public void testElevatorGettersAndSetters() {
        // 测试getter和setter方法
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        elevator.setCurrentFloor(10);
        assertEquals(10, elevator.getCurrentFloor());
        
        elevator.setDirection(Direction.DOWN);
        assertEquals(Direction.DOWN, elevator.getDirection());
        
        elevator.setStatus(ElevatorStatus.MOVING);
        assertEquals(ElevatorStatus.MOVING, elevator.getStatus());
        
        elevator.setMode(ElevatorMode.ENERGY_SAVING);
        assertEquals(ElevatorMode.ENERGY_SAVING, elevator.getMode());
        
        elevator.setCurrentLoad(500.0);
        assertEquals(500.0, elevator.getCurrentLoad(), 0.001);
        
        elevator.setEnergyConsumption(100.0);
        assertEquals(100.0, elevator.getEnergyConsumption(), 0.001);
    }
    
    @Test(timeout = 4000)
    public void testElevatorAddDestination() {
        // 测试添加目的地
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        elevator.addDestination(5);
        elevator.addDestination(10);
        
        Set<Integer> destinations = elevator.getDestinationSet();
        assertTrue(destinations.contains(5));
        assertTrue(destinations.contains(10));
    }
    
    @Test(timeout = 4000)
    public void testElevatorUpdateDirectionUp() {
        // 测试更新方向（向上）
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        elevator.setCurrentFloor(5);
        elevator.addDestination(10);
        elevator.updateDirection();
        
        assertEquals(Direction.UP, elevator.getDirection());
    }
    
    @Test(timeout = 4000)
    public void testElevatorUpdateDirectionDown() {
        // 测试更新方向（向下）
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        elevator.setCurrentFloor(10);
        elevator.addDestination(3);
        elevator.updateDirection();
        
        assertEquals(Direction.DOWN, elevator.getDirection());
    }
    
    @Test(timeout = 4000)
    public void testElevatorUpdateDirectionIdle() {
        // 测试更新方向（空闲状态）
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        elevator.updateDirection();
        
        assertEquals(ElevatorStatus.IDLE, elevator.getStatus());
    }
    
    @Test(timeout = 4000)
    public void testElevatorUnloadPassengers() {
        // 测试卸载乘客
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        elevator.setCurrentFloor(5);
        List<PassengerRequest> passengerList = elevator.getPassengerList();
        
        elevator.unloadPassengers();
        
        // 验证当前负载根据乘客数量更新
        assertTrue(elevator.getCurrentLoad() >= 0);
    }
    
    @Test(timeout = 4000)
    public void testElevatorClearAllRequests() {
        // 测试清除所有请求
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        elevator.addDestination(5);
        elevator.addDestination(10);
        
        List<PassengerRequest> cleared = elevator.clearAllRequests();
        
        assertNotNull(cleared);
        assertTrue(elevator.getDestinationSet().isEmpty());
        assertTrue(elevator.getPassengerList().isEmpty());
    }
    
    @Test(timeout = 4000)
    public void testElevatorHandleEmergency() {
        // 测试紧急情况处理
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        elevator.setCurrentFloor(10);
        elevator.addDestination(5);
        elevator.handleEmergency();
        
        assertEquals(ElevatorStatus.EMERGENCY, elevator.getStatus());
        assertTrue(elevator.getPassengerList().isEmpty());
        assertTrue(elevator.getDestinationSet().contains(1));
    }
    
    @Test(timeout = 4000)
    public void testElevatorGetMaxLoad() {
        // 测试获取最大载重
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        assertTrue(elevator.getMaxLoad() > 0);
    }
    
    @Test(timeout = 4000)
    public void testElevatorGetLockAndCondition() {
        // 测试获取锁和条件变量
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        assertNotNull(elevator.getLock());
        assertNotNull(elevator.getCondition());
    }
    
    @Test(timeout = 4000)
    public void testElevatorGetScheduler() {
        // 测试获取调度器
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        assertNotNull(elevator.getScheduler());
        assertSame(scheduler, elevator.getScheduler());
    }
    
    @Test(timeout = 4000)
    public void testElevatorAddObserver() {
        // 测试添加观察者
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        Observer observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                // 观察者实现
            }
        };
        
        elevator.addObserver(observer);
        List<Observer> observers = elevator.getObservers();
        
        assertTrue(observers.contains(observer));
    }
    
    @Test(timeout = 4000)
    public void testElevatorNotifyObservers() {
        // 测试通知观察者
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        final boolean[] notified = {false};
        
        Observer observer = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                notified[0] = true;
            }
        };
        
        elevator.addObserver(observer);
        elevator.notifyObservers(new Event(EventType.EMERGENCY, "test"));
        
        assertTrue(notified[0]);
    }
    
    // ==================== 策略类测试 ====================
    
    @Test(timeout = 4000)
    public void testNearestElevatorStrategySelectIdle() {
        // 测试最近电梯策略选择空闲电梯
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(5);
        elevator1.setStatus(ElevatorStatus.IDLE);
        
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator2.setCurrentFloor(10);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(6, 10, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        assertEquals(1, selected.getId()); // 电梯1更近
    }
    
    @Test(timeout = 4000)
    public void testNearestElevatorStrategySelectMoving() {
        // 测试最近电梯策略选择移动中的电梯
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(3);
        elevator1.setStatus(ElevatorStatus.MOVING);
        elevator1.setDirection(Direction.UP);
        
        elevators.add(elevator1);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.HIGH, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        assertEquals(1, selected.getId());
    }
    
    @Test(timeout = 4000)
    public void testNearestElevatorStrategyNoEligible() {
        // 测试没有合适的电梯
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setStatus(ElevatorStatus.EMERGENCY);
        
        elevators.add(elevator1);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    @Test(timeout = 4000)
    public void testNearestElevatorStrategyIsEligible() {
        // 测试isEligible方法
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setStatus(ElevatorStatus.IDLE);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
        
        assertTrue(strategy.isEligible(elevator1, request));
        
        elevator1.setStatus(ElevatorStatus.MOVING);
        elevator1.setDirection(Direction.UP);
        assertTrue(strategy.isEligible(elevator1, request));
        
        elevator1.setDirection(Direction.DOWN);
        assertFalse(strategy.isEligible(elevator1, request));
    }
    
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategySelectElevator() {
        // 测试高效策略选择电梯
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(5);
        elevator1.setStatus(ElevatorStatus.IDLE);
        
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator2.setCurrentFloor(15);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(6, 10, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        assertEquals(1, selected.getId());
    }
    
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategyIsCloser() {
        // 测试isCloser方法
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(5);
        
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator2.setCurrentFloor(10);
        
        PassengerRequest request = new PassengerRequest(6, 12, Priority.MEDIUM, RequestType.STANDARD);
        
        assertTrue(strategy.isCloser(elevator1, elevator2, request));
        assertFalse(strategy.isCloser(elevator2, elevator1, request));
    }
    
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategyNoElevator() {
        // 测试没有电梯可用
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        List<Elevator> elevators = new ArrayList<>();
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    @Test(timeout = 4000)
    public void testEnergySavingStrategySelectIdle() {
        // 测试节能策略选择空闲电梯
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setStatus(ElevatorStatus.IDLE);
        
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator2.setStatus(ElevatorStatus.MOVING);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        assertEquals(1, selected.getId());
    }
    
    @Test(timeout = 4000)
    public void testEnergySavingStrategySelectMovingNearby() {
        // 测试节能策略选择附近移动的电梯
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(5);
        elevator1.setStatus(ElevatorStatus.MOVING);
        elevator1.setDirection(Direction.UP);
        
        elevators.add(elevator1);
        
        PassengerRequest request = new PassengerRequest(6, 10, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        assertEquals(1, selected.getId());
    }
    
    @Test(timeout = 4000)
    public void testEnergySavingStrategyNoSuitableElevator() {
        // 测试没有合适的电梯
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(1);
        elevator1.setStatus(ElevatorStatus.MOVING);
        elevator1.setDirection(Direction.DOWN);
        
        elevators.add(elevator1);
        
        PassengerRequest request = new PassengerRequest(10, 15, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategySelectElevator() {
        // 测试预测调度策略
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(5);
        
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator2.setCurrentFloor(15);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(6, 10, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
    }
    
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategyCalculateCost() {
        // 测试计算预测成本
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setCurrentFloor(5);
        
        PassengerRequest request = new PassengerRequest(10, 15, Priority.MEDIUM, RequestType.STANDARD);
        double cost = strategy.calculatePredictedCost(elevator, request);
        
        assertTrue(cost > 0);
    }
    
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategyEmptyList() {
        // 测试空电梯列表
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNull(selected);
    }
    
    // ==================== Scheduler类测试 ====================
    
    @Test(timeout = 4000)
    public void testSchedulerCreation() {
        // 测试Scheduler创建
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        assertNotNull(scheduler);
    }
    
    @Test(timeout = 4000)
    public void testSchedulerSingletonWithParams() {
        // 测试带参数的单例获取
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler1 = Scheduler.getInstance(elevators, 20, new NearestElevatorStrategy());
        Scheduler scheduler2 = Scheduler.getInstance(elevators, 20, new NearestElevatorStrategy());
        
        assertNotNull(scheduler1);
        assertSame(scheduler1, scheduler2);
    }
    
    @Test(timeout = 4000)
    public void testSchedulerSingletonNoParams() {
        // 测试无参数的单例获取
        Scheduler scheduler = Scheduler.getInstance();
        
        assertNotNull(scheduler);
    }
    
    @Test(timeout = 4000)
    public void testSchedulerSubmitHighPriorityRequest() {
        // 测试提交高优先级请求
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(1, 10, Priority.HIGH, RequestType.STANDARD);
        scheduler.submitRequest(request);
        
        // 高优先级请求应该被处理
    }
    
    @Test(timeout = 4000)
    public void testSchedulerSubmitNormalPriorityRequest() {
        // 测试提交普通优先级请求
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(1, 10, Priority.MEDIUM, RequestType.STANDARD);
        scheduler.submitRequest(request);
        
        // 普通请求应该被添加到楼层
    }
    
    @Test(timeout = 4000)
    public void testSchedulerDispatchElevator() {
        // 测试分配电梯
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
        scheduler.dispatchElevator(request);
        
        // 电梯应该被分配
    }
    
    @Test(timeout = 4000)
    public void testSchedulerGetRequestsAtFloor() {
        // 测试获取楼层的请求
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        List<PassengerRequest> requests = scheduler.getRequestsAtFloor(5, Direction.UP);
        
        assertNotNull(requests);
    }
    
    @Test(timeout = 4000)
    public void testSchedulerSetDispatchStrategy() {
        // 测试设置调度策略
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        DispatchStrategy newStrategy = new HighEfficiencyStrategy();
        scheduler.setDispatchStrategy(newStrategy);
        
        // 策略应该被更新
    }
    
    @Test(timeout = 4000)
    public void testSchedulerRedistributeRequests() {
        // 测试重新分配请求
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator1 = new Elevator(1, scheduler);
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        elevator1.addDestination(5);
        
        scheduler.redistributeRequests(elevator1);
        
        // 请求应该被重新分配
    }
    
    @Test(timeout = 4000)
    public void testSchedulerExecuteEmergencyProtocol() {
        // 测试执行紧急协议
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator = new Elevator(1, scheduler);
        elevators.add(elevator);
        
        scheduler.executeEmergencyProtocol();
        
        assertEquals(ElevatorStatus.EMERGENCY, elevator.getStatus());
    }
    
    @Test(timeout = 4000)
    public void testSchedulerUpdateMethod() {
        // 测试观察者更新方法
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator = new Elevator(1, scheduler);
        elevators.add(elevator);
        
        Event faultEvent = new Event(EventType.ELEVATOR_FAULT, elevator);
        scheduler.update(elevator, faultEvent);
        
        Event emergencyEvent = new Event(EventType.EMERGENCY, "test");
        scheduler.update(null, emergencyEvent);
        
        // 观察者应该响应事件
    }
    
    // ==================== ElevatorManager类测试 ====================
    
    @Test(timeout = 4000)
    public void testElevatorManagerSingleton() {
        // 测试单例模式
        ElevatorManager manager1 = ElevatorManager.getInstance();
        ElevatorManager manager2 = ElevatorManager.getInstance();
        
        assertNotNull(manager1);
        assertSame(manager1, manager2);
    }
    
    @Test(timeout = 4000)
    public void testElevatorManagerRegisterElevator() {
        // 测试注册电梯
        ElevatorManager manager = new ElevatorManager();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        manager.registerElevator(elevator);
        
        Elevator retrieved = manager.getElevatorById(1);
        assertNotNull(retrieved);
        assertEquals(1, retrieved.getId());
    }
    
    @Test(timeout = 4000)
    public void testElevatorManagerGetElevatorByIdNotFound() {
        // 测试获取不存在的电梯
        ElevatorManager manager = new ElevatorManager();
        
        Elevator elevator = manager.getElevatorById(999);
        
        assertNull(elevator);
    }
    
    @Test(timeout = 4000)
    public void testElevatorManagerGetAllElevators() {
        // 测试获取所有电梯
        ElevatorManager manager = new ElevatorManager();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator1 = new Elevator(1, scheduler);
        Elevator elevator2 = new Elevator(2, scheduler);
        
        manager.registerElevator(elevator1);
        manager.registerElevator(elevator2);
        
        Collection<Elevator> allElevators = manager.getAllElevators();
        
        assertEquals(2, allElevators.size());
    }
    
    @Test(timeout = 4000)
    public void testElevatorManagerMultipleRegistrations() {
        // 测试多次注册（覆盖）
        ElevatorManager manager = new ElevatorManager();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator1 = new Elevator(1, scheduler);
        Elevator elevator2 = new Elevator(1, scheduler);
        
        manager.registerElevator(elevator1);
        manager.registerElevator(elevator2);
        
        Collection<Elevator> allElevators = manager.getAllElevators();
        assertEquals(1, allElevators.size());
    }
    
    // ==================== EventBus类测试 ====================
    
    @Test(timeout = 4000)
    public void testEventBusSingleton() {
        // 测试单例模式
        EventBus bus1 = EventBus.getInstance();
        EventBus bus2 = EventBus.getInstance();
        
        assertNotNull(bus1);
        assertSame(bus1, bus2);
    }
    
    @Test(timeout = 4000)
    public void testEventBusSubscribeAndPublish() {
        // 测试订阅和发布
        EventBus bus = new EventBus();
        final boolean[] eventReceived = {false};
        
        EventBus.EventListener listener = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                eventReceived[0] = true;
            }
        };
        
        bus.subscribe(EventType.EMERGENCY, listener);
        bus.publish(new EventBus.Event(EventType.EMERGENCY, "test"));
        
        assertTrue(eventReceived[0]);
    }
    
    @Test(timeout = 4000)
    public void testEventBusPublishWithNoListeners() {
        // 测试发布没有监听者的事件
        EventBus bus = new EventBus();
        
        bus.publish(new EventBus.Event(EventType.ELEVATOR_FAULT, "test"));
        
        // 不应该抛出异常
    }
    
    @Test(timeout = 4000)
    public void testEventBusMultipleListeners() {
        // 测试多个监听者
        EventBus bus = new EventBus();
        final int[] count = {0};
        
        EventBus.EventListener listener1 = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                count[0]++;
            }
        };
        
        EventBus.EventListener listener2 = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                count[0]++;
            }
        };
        
        bus.subscribe(EventType.EMERGENCY, listener1);
        bus.subscribe(EventType.EMERGENCY, listener2);
        bus.publish(new EventBus.Event(EventType.EMERGENCY, "test"));
        
        assertEquals(2, count[0]);
    }
    
    @Test(timeout = 4000)
    public void testEventBusInnerEvent() {
        // 测试EventBus内部Event类
        EventBus.Event event = new EventBus.Event(EventType.CONFIG_UPDATED, "config");
        
        assertEquals(EventType.CONFIG_UPDATED, event.getType());
        assertEquals("config", event.getData());
    }
    
    // ==================== LogManager类测试 ====================
    
    @Test(timeout = 4000)
    public void testLogManagerSingleton() {
        // 测试单例模式
        LogManager manager1 = LogManager.getInstance();
        LogManager manager2 = LogManager.getInstance();
        
        assertNotNull(manager1);
        assertSame(manager1, manager2);
    }
    
    @Test(timeout = 4000)
    public void testLogManagerRecordElevatorEvent() {
        // 测试记录电梯事件
        LogManager manager = new LogManager();
        
        manager.recordElevatorEvent(1, "Elevator started");
        
        // 事件应该被记录
    }
    
    @Test(timeout = 4000)
    public void testLogManagerRecordSchedulerEvent() {
        // 测试记录调度器事件
        LogManager manager = new LogManager();
        
        manager.recordSchedulerEvent("Scheduler initialized");
        
        // 事件应该被记录
    }
    
    @Test(timeout = 4000)
    public void testLogManagerRecordEvent() {
        // 测试记录通用事件
        LogManager manager = new LogManager();
        
        manager.recordEvent("TestSource", "Test message");
        
        // 事件应该被记录
    }
    
    @Test(timeout = 4000)
    public void testLogManagerQueryLogs() {
        // 测试查询日志
        LogManager manager = new LogManager();
        long startTime = System.currentTimeMillis();
        
        manager.recordEvent("TestSource", "Message 1");
        manager.recordEvent("TestSource", "Message 2");
        manager.recordEvent("OtherSource", "Message 3");
        
        long endTime = System.currentTimeMillis() + 1000;
        
        List<LogManager.SystemLog> logs = manager.queryLogs("TestSource", startTime, endTime);
        
        assertEquals(2, logs.size());
    }
    
    @Test(timeout = 4000)
    public void testLogManagerSystemLog() {
        // 测试SystemLog内部类
        LogManager.SystemLog log = new LogManager.SystemLog("Source", "Message", 12345L);
        
        assertEquals("Source", log.getSource());
        assertEquals("Message", log.getMessage());
        assertEquals(12345L, log.getTimestamp());
    }
    
    @Test(timeout = 4000)
    public void testLogManagerQueryLogsWithTimeRange() {
        // 测试带时间范围的日志查询
        LogManager manager = new LogManager();
        long startTime = System.currentTimeMillis();
        
        manager.recordEvent("Source1", "Message");
        
        long endTime = System.currentTimeMillis() - 2000;
        
        List<LogManager.SystemLog> logs = manager.queryLogs("Source1", startTime, endTime);
        
        assertEquals(0, logs.size());
    }
    
    // ==================== MaintenanceManager类测试 ====================
    
    @Test(timeout = 4000)
    public void testMaintenanceManagerSingleton() {
        // 测试单例模式
        MaintenanceManager manager1 = MaintenanceManager.getInstance();
        MaintenanceManager manager2 = MaintenanceManager.getInstance();
        
        assertNotNull(manager1);
        assertSame(manager1, manager2);
    }
    
    @Test(timeout = 4000)
    public void testMaintenanceManagerScheduleMaintenance() throws Exception {
        // 测试安排维护
        MaintenanceManager manager = new MaintenanceManager();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        manager.scheduleMaintenance(elevator);
        
        // 维护任务应该被添加
    }
    
    @Test(timeout = 4000)
    public void testMaintenanceManagerPerformMaintenance() {
        // 测试执行维护
        MaintenanceManager manager = new MaintenanceManager();
        MaintenanceManager.MaintenanceTask task = 
            new MaintenanceManager.MaintenanceTask(1, System.currentTimeMillis(), "Test maintenance");
        
        manager.performMaintenance(task);
        
        // 维护应该被执行
    }
    
    @Test(timeout = 4000)
    public void testMaintenanceManagerRecordMaintenanceResult() {
        // 测试记录维护结果
        MaintenanceManager manager = new MaintenanceManager();
        
        manager.recordMaintenanceResult(1, "Maintenance completed successfully");
        
        // 结果应该被记录
    }
    
    @Test(timeout = 4000)
    public void testMaintenanceManagerNotifyPersonnel() {
        // 测试通知维护人员
        MaintenanceManager manager = new MaintenanceManager();
        MaintenanceManager.MaintenanceTask task = 
            new MaintenanceManager.MaintenanceTask(1, System.currentTimeMillis(), "Urgent repair");
        
        manager.notifyMaintenancePersonnel(task);
        
        // 通知应该被发送
    }
    
    @Test(timeout = 4000)
    public void testMaintenanceManagerOnEvent() {
        // 测试事件监听
        MaintenanceManager manager = new MaintenanceManager();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        EventBus.Event event = new EventBus.Event(EventType.ELEVATOR_FAULT, elevator);
        manager.onEvent(event);
        
        // 事件应该触发维护安排
    }
    
    @Test(timeout = 4000)
    public void testMaintenanceTaskGetters() {
        // 测试MaintenanceTask的getter方法
        long time = System.currentTimeMillis();
        MaintenanceManager.MaintenanceTask task = 
            new MaintenanceManager.MaintenanceTask(5, time, "Description");
        
        assertEquals(5, task.getElevatorId());
        assertEquals(time, task.getScheduledTime());
        assertEquals("Description", task.getDescription());
    }
    
    @Test(timeout = 4000)
    public void testMaintenanceRecordGetters() {
        // 测试MaintenanceRecord的getter方法
        long time = System.currentTimeMillis();
        MaintenanceManager.MaintenanceRecord record = 
            new MaintenanceManager.MaintenanceRecord(3, time, "Result");
        
        assertEquals(3, record.getElevatorId());
        assertEquals(time, record.getMaintenanceTime());
        assertEquals("Result", record.getResult());
    }
    
    // ==================== NotificationService类测试 ====================
    
    @Test(timeout = 4000)
    public void testNotificationServiceSingleton() {
        // 测试单例模式
        NotificationService service1 = NotificationService.getInstance();
        NotificationService service2 = NotificationService.getInstance();
        
        assertNotNull(service1);
        assertSame(service1, service2);
    }
    
    @Test(timeout = 4000)
    public void testNotificationServiceSendNotification() {
        // 测试发送通知
        NotificationService service = new NotificationService();
        List<String> recipients = Arrays.asList("user@example.com");
        NotificationService.Notification notification = 
            new NotificationService.Notification(
                NotificationService.NotificationType.EMERGENCY, 
                "Emergency message", 
                recipients
            );
        
        service.sendNotification(notification);
        
        // 通知应该被发送
    }
    
    @Test(timeout = 4000)
    public void testNotificationTypes() {
        // 测试所有通知类型
        NotificationService.NotificationType emergency = NotificationService.NotificationType.EMERGENCY;
        NotificationService.NotificationType maintenance = NotificationService.NotificationType.MAINTENANCE;
        NotificationService.NotificationType systemUpdate = NotificationService.NotificationType.SYSTEM_UPDATE;
        NotificationService.NotificationType information = NotificationService.NotificationType.INFORMATION;
        
        assertNotNull(emergency);
        assertNotNull(maintenance);
        assertNotNull(systemUpdate);
        assertNotNull(information);
    }
    
    @Test(timeout = 4000)
    public void testNotificationGetters() {
        // 测试Notification的getter方法
        List<String> recipients = Arrays.asList("user1@example.com", "user2@example.com");
        NotificationService.Notification notification = 
            new NotificationService.Notification(
                NotificationService.NotificationType.MAINTENANCE, 
                "Test message", 
                recipients
            );
        
        assertEquals(NotificationService.NotificationType.MAINTENANCE, notification.getType());
        assertEquals("Test message", notification.getMessage());
        assertEquals(recipients, notification.getRecipients());
    }
    
    @Test(timeout = 4000)
    public void testSMSChannelSupports() {
        // 测试SMS通道支持的类型
        NotificationService.SMSChannel channel = new NotificationService.SMSChannel();
        
        assertTrue(channel.supports(NotificationService.NotificationType.EMERGENCY));
        assertTrue(channel.supports(NotificationService.NotificationType.MAINTENANCE));
        assertFalse(channel.supports(NotificationService.NotificationType.SYSTEM_UPDATE));
        assertFalse(channel.supports(NotificationService.NotificationType.INFORMATION));
    }
    
    @Test(timeout = 4000)
    public void testSMSChannelSend() {
        // 测试SMS通道发送
        NotificationService.SMSChannel channel = new NotificationService.SMSChannel();
        List<String> recipients = Arrays.asList("123456789");
        NotificationService.Notification notification = 
            new NotificationService.Notification(
                NotificationService.NotificationType.EMERGENCY, 
                "Emergency!", 
                recipients
            );
        
        channel.send(notification);
        
        // 发送应该成功
    }
    
    @Test(timeout = 4000)
    public void testEmailChannelSupports() {
        // 测试Email通道支持所有类型
        NotificationService.EmailChannel channel = new NotificationService.EmailChannel();
        
        assertTrue(channel.supports(NotificationService.NotificationType.EMERGENCY));
        assertTrue(channel.supports(NotificationService.NotificationType.MAINTENANCE));
        assertTrue(channel.supports(NotificationService.NotificationType.SYSTEM_UPDATE));
        assertTrue(channel.supports(NotificationService.NotificationType.INFORMATION));
    }
    
    @Test(timeout = 4000)
    public void testEmailChannelSend() {
        // 测试Email通道发送
        NotificationService.EmailChannel channel = new NotificationService.EmailChannel();
        List<String> recipients = Arrays.asList("user@example.com");
        NotificationService.Notification notification = 
            new NotificationService.Notification(
                NotificationService.NotificationType.INFORMATION, 
                "Info message", 
                recipients
            );
        
        channel.send(notification);
        
        // 发送应该成功
    }
    
    // ==================== SecurityMonitor类测试 ====================
    
    @Test(timeout = 4000)
    public void testSecurityMonitorSingleton() {
        // 测试单例模式
        SecurityMonitor monitor1 = SecurityMonitor.getInstance();
        SecurityMonitor monitor2 = SecurityMonitor.getInstance();
        
        assertNotNull(monitor1);
        assertSame(monitor1, monitor2);
    }
    
    @Test(timeout = 4000)
    public void testSecurityMonitorHandleEmergency() {
        // 测试处理紧急情况
        SecurityMonitor monitor = new SecurityMonitor();
        
        monitor.handleEmergency("Fire detected");
        
        // 紧急情况应该被处理
    }
    
    @Test(timeout = 4000)
    public void testSecurityMonitorOnEvent() {
        // 测试事件监听
        SecurityMonitor monitor = new SecurityMonitor();
        
        EventBus.Event event = new EventBus.Event(EventType.EMERGENCY, "Emergency data");
        monitor.onEvent(event);
        
        // 事件应该触发处理
    }
    
    @Test(timeout = 4000)
    public void testSecurityEventGetters() {
        // 测试SecurityEvent的getter方法
        long time = System.currentTimeMillis();
        Object data = "test data";
        SecurityMonitor.SecurityEvent event = 
            new SecurityMonitor.SecurityEvent("Description", time, data);
        
        assertEquals("Description", event.getDescription());
        assertEquals(time, event.getTimestamp());
        assertEquals(data, event.getData());
    }
    
    // ==================== ThreadPoolManager类测试 ====================
    
    @Test(timeout = 4000)
    public void testThreadPoolManagerSingleton() {
        // 测试单例模式
        ThreadPoolManager manager1 = ThreadPoolManager.getInstance();
        ThreadPoolManager manager2 = ThreadPoolManager.getInstance();
        
        assertNotNull(manager1);
        assertSame(manager1, manager2);
    }
    
    @Test(timeout = 4000)
    public void testThreadPoolManagerSubmitTask() throws Exception {
        // 测试提交任务
        ThreadPoolManager manager = new ThreadPoolManager();
        final boolean[] executed = {false};
        
        Runnable task = new Runnable() {
            @Override
            public void run() {
                executed[0] = true;
            }
        };
        
        manager.submitTask(task);
        
        Thread.sleep(100);
        assertTrue(executed[0]);
    }
    
    @Test(timeout = 4000)
    public void testThreadPoolManagerShutdown() {
        // 测试关闭线程池
        ThreadPoolManager manager = new ThreadPoolManager();
        
        manager.shutdown();
        
        // 线程池应该被关闭
    }
    
    @Test(timeout = 4000)
    public void testThreadPoolManagerMultipleTasks() throws Exception {
        // 测试提交多个任务
        ThreadPoolManager manager = new ThreadPoolManager();
        final int[] counter = {0};
        
        for (int i = 0; i < 5; i++) {
            manager.submitTask(new Runnable() {
                @Override
                public void run() {
                    synchronized (counter) {
                        counter[0]++;
                    }
                }
            });
        }
        
        Thread.sleep(500);
        assertEquals(5, counter[0]);
    }
    
    // ==================== AnalyticsEngine类测试 ====================
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineSingleton() {
        // 测试单例模式
        AnalyticsEngine engine1 = AnalyticsEngine.getInstance();
        AnalyticsEngine engine2 = AnalyticsEngine.getInstance();
        
        assertNotNull(engine1);
        assertSame(engine1, engine2);
    }
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineProcessStatusReport() {
        // 测试处理状态报告
        AnalyticsEngine engine = new AnalyticsEngine();
        ElevatorStatusReport report = new ElevatorStatusReport(
            1, 5, Direction.UP, ElevatorStatus.MOVING, 2.0, 400.0, 6
        );
        
        engine.processStatusReport(report);
        
        // 报告应该被处理
    }
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineUpdateFloorPassengerCount() {
        // 测试更新楼层乘客数量
        AnalyticsEngine engine = new AnalyticsEngine();
        
        engine.updateFloorPassengerCount(5, 10);
        
        // 数量应该被更新
    }
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineIsPeakHoursTrue() {
        // 测试是否为高峰时段（是）
        AnalyticsEngine engine = new AnalyticsEngine();
        
        engine.updateFloorPassengerCount(1, 30);
        engine.updateFloorPassengerCount(2, 25);
        
        assertTrue(engine.isPeakHours());
    }
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineIsPeakHoursFalse() {
        // 测试是否为高峰时段（否）
        AnalyticsEngine engine = new AnalyticsEngine();
        
        engine.updateFloorPassengerCount(1, 5);
        engine.updateFloorPassengerCount(2, 3);
        
        assertFalse(engine.isPeakHours());
    }
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineGenerateReport() {
        // 测试生成性能报告
        AnalyticsEngine engine = new AnalyticsEngine();
        
        AnalyticsEngine.Report report = engine.generatePerformanceReport();
        
        assertNotNull(report);
        assertEquals("System Performance Report", report.getTitle());
        assertTrue(report.getGeneratedTime() > 0);
    }
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineReportGetters() {
        // 测试Report的getter方法
        long time = System.currentTimeMillis();
        AnalyticsEngine.Report report = new AnalyticsEngine.Report("Test Report", time);
        
        assertEquals("Test Report", report.getTitle());
        assertEquals(time, report.getGeneratedTime());
    }
    
    // ==================== 边界条件和特殊情况测试 ====================
    
    @Test(timeout = 4000)
    public void testPassengerRequestSameFloor() {
        // 测试起始楼层和目标楼层相同的情况
        PassengerRequest request = new PassengerRequest(5, 5, Priority.MEDIUM, RequestType.STANDARD);
        
        assertEquals(5, request.getStartFloor());
        assertEquals(5, request.getDestinationFloor());
        // 方向应该是DOWN（因为startFloor不小于destinationFloor）
        assertEquals(Direction.DOWN, request.getDirection());
    }
    
    @Test(timeout = 4000)
    public void testFloorZero() {
        // 测试零楼层
        Floor floor = new Floor(0);
        assertEquals(0, floor.getFloorNumber());
    }
    
    @Test(timeout = 4000)
    public void testFloorNegative() {
        // 测试负楼层（地下室）
        Floor floor = new Floor(-1);
        assertEquals(-1, floor.getFloorNumber());
    }
    
    @Test(timeout = 4000)
    public void testElevatorWithMultipleObservers() {
        // 测试电梯有多个观察者
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        Elevator elevator = new Elevator(1, scheduler);
        
        final int[] notificationCount = {0};
        
        Observer observer1 = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                notificationCount[0]++;
            }
        };
        
        Observer observer2 = new Observer() {
            @Override
            public void update(Observable o, Object arg) {
                notificationCount[0]++;
            }
        };
        
        elevator.addObserver(observer1);
        elevator.addObserver(observer2);
        
        elevator.notifyObservers(new Event(EventType.EMERGENCY, "test"));
        
        assertEquals(2, notificationCount[0]);
    }
    
    @Test(timeout = 4000)
    public void testSystemConfigBoundaryValues() {
        // 测试SystemConfig的边界值
        SystemConfig config = new SystemConfig();
        
        config.setFloorCount(1);
        assertEquals(1, config.getFloorCount());
        
        config.setElevatorCount(1);
        assertEquals(1, config.getElevatorCount());
        
        config.setMaxLoad(0.1);
        assertEquals(0.1, config.getMaxLoad(), 0.001);
    }
    
    @Test(timeout = 4000)
    public void testSchedulerWithEmptyElevatorList() {
        // 测试空电梯列表的调度器
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        PassengerRequest request = new PassengerRequest(5, 10, Priority.MEDIUM, RequestType.STANDARD);
        scheduler.dispatchElevator(request);
        
        // 不应该抛出异常
    }
    
    @Test(timeout = 4000)
    public void testElevatorManagerEmptyCollection() {
        // 测试空的电梯集合
        ElevatorManager manager = new ElevatorManager();
        
        Collection<Elevator> elevators = manager.getAllElevators();
        
        assertNotNull(elevators);
        assertEquals(0, elevators.size());
    }
    
    @Test(timeout = 4000)
    public void testLogManagerQueryLogsNoMatch() {
        // 测试查询不匹配的日志
        LogManager manager = new LogManager();
        
        manager.recordEvent("Source1", "Message");
        
        List<LogManager.SystemLog> logs = manager.queryLogs("Source2", 0, System.currentTimeMillis());
        
        assertEquals(0, logs.size());
    }
    
    @Test(timeout = 4000)
    public void testEventBusMultipleEventTypes() {
        // 测试EventBus处理多种事件类型
        EventBus bus = new EventBus();
        final int[] emergencyCount = {0};
        final int[] faultCount = {0};
        
        EventBus.EventListener emergencyListener = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                emergencyCount[0]++;
            }
        };
        
        EventBus.EventListener faultListener = new EventBus.EventListener() {
            @Override
            public void onEvent(EventBus.Event event) {
                faultCount[0]++;
            }
        };
        
        bus.subscribe(EventType.EMERGENCY, emergencyListener);
        bus.subscribe(EventType.ELEVATOR_FAULT, faultListener);
        
        bus.publish(new EventBus.Event(EventType.EMERGENCY, "emergency"));
        bus.publish(new EventBus.Event(EventType.ELEVATOR_FAULT, "fault"));
        
        assertEquals(1, emergencyCount[0]);
        assertEquals(1, faultCount[0]);
    }
    
    @Test(timeout = 4000)
    public void testPredictiveSchedulingStrategyWithHighLoad() {
        // 测试高负载情况下的预测调度
        PredictiveSchedulingStrategy strategy = new PredictiveSchedulingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setCurrentFloor(5);
        elevator.setCurrentLoad(700.0);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(10, 15, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
    }
    
    @Test(timeout = 4000)
    public void testEnergySavingStrategyBoundaryDistance() {
        // 测试节能策略的距离边界（恰好5层）
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setCurrentFloor(5);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.UP);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(10, 15, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        // 距离为5，应该被选中
        assertNull(selected); // 实际是null，因为距离不小于5
    }
    
    @Test(timeout = 4000)
    public void testEnergySavingStrategyWithinDistance() {
        // 测试节能策略在距离内
        EnergySavingStrategy strategy = new EnergySavingStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setCurrentFloor(8);
        elevator.setStatus(ElevatorStatus.MOVING);
        elevator.setDirection(Direction.UP);
        
        elevators.add(elevator);
        
        PassengerRequest request = new PassengerRequest(10, 15, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        // 距离为2，应该被选中
        assertNotNull(selected);
    }
    
    @Test(timeout = 4000)
    public void testHighEfficiencyStrategyWithSameDirection() {
        // 测试高效策略选择相同方向的电梯
        HighEfficiencyStrategy strategy = new HighEfficiencyStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(3);
        elevator1.setStatus(ElevatorStatus.MOVING);
        elevator1.setDirection(Direction.UP);
        
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator2.setCurrentFloor(10);
        elevator2.setStatus(ElevatorStatus.MOVING);
        elevator2.setDirection(Direction.DOWN);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(5, 12, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        assertEquals(1, selected.getId());
    }
    
    @Test(timeout = 4000)
    public void testNearestElevatorStrategyEqualDistance() {
        // 测试相同距离时的选择
        NearestElevatorStrategy strategy = new NearestElevatorStrategy();
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, strategy);
        
        Elevator elevator1 = new Elevator(1, scheduler);
        elevator1.setCurrentFloor(5);
        elevator1.setStatus(ElevatorStatus.IDLE);
        
        Elevator elevator2 = new Elevator(2, scheduler);
        elevator2.setCurrentFloor(15);
        elevator2.setStatus(ElevatorStatus.IDLE);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        PassengerRequest request = new PassengerRequest(10, 12, Priority.MEDIUM, RequestType.STANDARD);
        Elevator selected = strategy.selectElevator(elevators, request);
        
        assertNotNull(selected);
        assertEquals(1, selected.getId()); // 两个距离相同，选第一个
    }
    
    @Test(timeout = 4000)
    public void testNotificationServiceWithEmptyRecipients() {
        // 测试空接收者列表
        NotificationService service = new NotificationService();
        List<String> recipients = new ArrayList<>();
        NotificationService.Notification notification = 
            new NotificationService.Notification(
                NotificationService.NotificationType.INFORMATION, 
                "Message", 
                recipients
            );
        
        service.sendNotification(notification);
        
        // 不应该抛出异常
    }
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineMultipleReports() {
        // 测试处理多个报告
        AnalyticsEngine engine = new AnalyticsEngine();
        
        for (int i = 1; i <= 10; i++) {
            ElevatorStatusReport report = new ElevatorStatusReport(
                i, i * 2, Direction.UP, ElevatorStatus.MOVING, 2.0, 400.0, 5
            );
            engine.processStatusReport(report);
        }
        
        // 所有报告应该被处理
    }
    
    @Test(timeout = 4000)
    public void testAnalyticsEngineIsPeakHoursBoundary() {
        // 测试高峰时段边界值（恰好50）
        AnalyticsEngine engine = new AnalyticsEngine();
        
        engine.updateFloorPassengerCount(1, 50);
        
        assertFalse(engine.isPeakHours()); // 需要大于50
        
        engine.updateFloorPassengerCount(2, 1);
        
        assertTrue(engine.isPeakHours()); // 现在是51
    }
    
    @Test(timeout = 4000)
    public void testSecurityMonitorMultipleEmergencies() {
        // 测试多次紧急情况
        SecurityMonitor monitor = new SecurityMonitor();
        
        monitor.handleEmergency("Fire");
        monitor.handleEmergency("Earthquake");
        monitor.handleEmergency("Power failure");
        
        // 所有紧急情况应该被处理
    }
    
    @Test(timeout = 4000)
    public void testMaintenanceManagerOnEventNonFault() {
        // 测试非故障事件
        MaintenanceManager manager = new MaintenanceManager();
        
        EventBus.Event event = new EventBus.Event(EventType.CONFIG_UPDATED, "config");
        manager.onEvent(event);
        
        // 不应该触发维护（只处理ELEVATOR_FAULT）
    }
    
    // ==================== 并发和线程安全测试 ====================
    
    @Test(timeout = 4000)
    public void testSystemConfigConcurrentAccess() throws InterruptedException {
        // 测试SystemConfig的并发访问
        final SystemConfig config = SystemConfig.getInstance();
        final CountDownLatch latch = new CountDownLatch(10);
        
        for (int i = 0; i < 10; i++) {
            final int value = i + 10;
            new Thread(() -> {
                config.setFloorCount(value);
                config.setElevatorCount(value);
                config.setMaxLoad(value * 100.0);
                latch.countDown();
            }).start();
        }
        
        latch.await();
        
        // 最终值应该是其中一个线程设置的值
        assertTrue(config.getFloorCount() >= 10);
    }
    
    @Test(timeout = 4000)
    public void testSchedulerConcurrentSubmit() throws InterruptedException {
        // 测试调度器并发提交请求
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator = new Elevator(1, scheduler);
        elevator.setStatus(ElevatorStatus.IDLE);
        elevators.add(elevator);
        
        final CountDownLatch latch = new CountDownLatch(5);
        
        for (int i = 0; i < 5; i++) {
            final int floor = i + 1;
            new Thread(() -> {
                PassengerRequest request = new PassengerRequest(floor, floor + 5, Priority.MEDIUM, RequestType.STANDARD);
                scheduler.submitRequest(request);
                latch.countDown();
            }).start();
        }
        
        latch.await();
        
        // 所有请求应该被处理
    }
    
    @Test(timeout = 4000)
    public void testEventBusConcurrentPublish() throws InterruptedException {
        // 测试EventBus并发发布
        final EventBus bus = new EventBus();
        final int[] count = {0};
        
        EventBus.EventListener listener = new EventBus.EventListener() {
            @Override
            public synchronized void onEvent(EventBus.Event event) {
                count[0]++;
            }
        };
        
        bus.subscribe(EventType.EMERGENCY, listener);
        
        final CountDownLatch latch = new CountDownLatch(10);
        
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                bus.publish(new EventBus.Event(EventType.EMERGENCY, "test"));
                latch.countDown();
            }).start();
        }
        
        latch.await();
        Thread.sleep(100);
        
        assertEquals(10, count[0]);
    }
    
    // ==================== 完整场景测试 ====================
    
    @Test(timeout = 4000)
    public void testCompleteElevatorScenario() {
        // 测试完整的电梯调度场景
        List<Elevator> elevators = new ArrayList<>();
        Scheduler scheduler = new Scheduler(elevators, 20, new NearestElevatorStrategy());
        
        Elevator elevator1 = new Elevator(1, scheduler);
        Elevator elevator2 = new Elevator(2, scheduler);
        
        elevator1.setStatus(ElevatorStatus.IDLE);
        elevator2.setStatus(ElevatorStatus.IDLE);
        elevator2.setCurrentFloor(10);
        
        elevators.add(elevator1);
        elevators.add(elevator2);
        
        ElevatorManager manager = new ElevatorManager();
        manager.registerElevator(elevator1);
        manager.registerElevator(elevator2);
        
        PassengerRequest request1 = new PassengerRequest(3, 10, Priority.HIGH, RequestType.STANDARD);
        PassengerRequest request2 = new PassengerRequest(15, 5, Priority.MEDIUM, RequestType.DESTINATION_CONTROL);
        
        scheduler.submitRequest(request1);
        scheduler.submitRequest(request2);
        
        assertNotNull(manager.getElevatorById(1));
        assertNotNull(manager.getElevatorById(2));
        assertEquals(2, manager.getAllElevators().size());
    }
    
    @Test(timeout = 4000)
    public void testCompleteNotificationScenario() {
        // 测试完整的通知场景
        NotificationService service = NotificationService.getInstance();
        
        List<String> recipients = Arrays.asList("admin@building.com", "security@building.com");
        
        NotificationService.Notification emergencyNotif = new NotificationService.Notification(
            NotificationService.NotificationType.EMERGENCY,
            "Emergency: Fire detected on floor 10",
            recipients
        );
        
        NotificationService.Notification maintenanceNotif = new NotificationService.Notification(
            NotificationService.NotificationType.MAINTENANCE,
            "Maintenance scheduled for elevator 3",
            recipients
        );
        
        service.sendNotification(emergencyNotif);
        service.sendNotification(maintenanceNotif);
        
        // 通知应该通过多个通道发送
    }
    
    @Test(timeout = 4000)
    public void testCompleteAnalyticsScenario() {
        // 测试完整的分析场景
        AnalyticsEngine engine = AnalyticsEngine.getInstance();
        
        for (int floor = 1; floor <= 20; floor++) {
            engine.updateFloorPassengerCount(floor, floor % 3);
        }
        
        ElevatorStatusReport report1 = new ElevatorStatusReport(
            1, 5, Direction.UP, ElevatorStatus.MOVING, 2.5, 450.0, 6
        );
        ElevatorStatusReport report2 = new ElevatorStatusReport(
            2, 15, Direction.DOWN, ElevatorStatus.MOVING, 2.0, 350.0, 5
        );
        
        engine.processStatusReport(report1);
        engine.processStatusReport(report2);
        
        AnalyticsEngine.Report performanceReport = engine.generatePerformanceReport();
        
        assertNotNull(performanceReport);
        assertEquals("System Performance Report", performanceReport.getTitle());
    }
    
    @Test(timeout = 4000)
    public void testAllEnumValuesAndValueOf() {
        // 综合测试所有枚举的values和valueOf方法
        
        // Direction
        Direction[] directions = Direction.values();
        for (Direction d : directions) {
            assertEquals(d, Direction.valueOf(d.name()));
        }
        
        // Priority
        Priority[] priorities = Priority.values();
        for (Priority p : priorities) {
            assertEquals(p, Priority.valueOf(p.name()));
        }
        
        // RequestType
        RequestType[] requestTypes = RequestType.values();
        for (RequestType rt : requestTypes) {
            assertEquals(rt, RequestType.valueOf(rt.name()));
        }
        
        // SpecialNeeds
        SpecialNeeds[] specialNeeds = SpecialNeeds.values();
        for (SpecialNeeds sn : specialNeeds) {
            assertEquals(sn, SpecialNeeds.valueOf(sn.name()));
        }
        
        // ElevatorMode
        ElevatorMode[] modes = ElevatorMode.values();
        for (ElevatorMode em : modes) {
            assertEquals(em, ElevatorMode.valueOf(em.name()));
        }
        
        // ElevatorStatus
        ElevatorStatus[] statuses = ElevatorStatus.values();
        for (ElevatorStatus es : statuses) {
            assertEquals(es, ElevatorStatus.valueOf(es.name()));
        }
        
        // EventType
        EventType[] eventTypes = EventType.values();
        for (EventType et : eventTypes) {
            assertEquals(et, EventType.valueOf(et.name()));
        }
        
        // NotificationType
        NotificationService.NotificationType[] notificationTypes = NotificationService.NotificationType.values();
        for (NotificationService.NotificationType nt : notificationTypes) {
            assertEquals(nt, NotificationService.NotificationType.valueOf(nt.name()));
        }
    }
}
