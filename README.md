# 개인 프로젝트 : Taxi

![MzA2Mjc1Ng](https://user-images.githubusercontent.com/24731820/120581354-a4c44800-c465-11eb-8bf9-6a753f43556d.jpeg)

콜택시 서비스를 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 프로젝트임

- 체크포인트 : https://workflowy.com/s/assessment/qJn45fBdVZn4atl3


# Table of contents

- [예제 - Taxi](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [분석/설계](#분석설계)
    - [Event Storming 결과](#Event-Storming-결과)
    - [완성된 1차 모형](#완성된-1차-모형)
    - [바운디드 컨텍스트](#바운디드-컨텍스트)
    - [기능적 요구사항 검증](#기능적-요구사항을-커버하는지-검증)
    - [비기능적 요구사항 검증](#비기능-요구사항에-대한-검증)
    - [헥사고날 아키텍처 다이어그램 도출](#헥사고날-아키텍처-다이어그램-도출)
       
  - [구현:](#구현)
    - [DDD 의 적용](#ddd-의-적용)   
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#동기식-호출-과-Fallback-처리)
    
  - [운영](#운영)
    - [CI/CD 설정](#CICD-설정)
    - [Kubernetes 설정](#Kubernetes-설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출/서킷-브레이킹/장애격리)
    - [오토스케일 아웃](#Autoscale-HPA)
    - [무정지 재배포](#Zero-downtime-deploy)
 
 

# 서비스 시나리오

[ 기능적 요구사항 ]
1. 승객이 택시를 호출한다.
2. 콜 목록에 고객의 콜이 쌓인다.
3. 택시 기사에게 콜이 전송된다.
4. 택시 기사가 콜을 승락한다.
5. 콜 목록에서 해당 콜이 배차완료 콜이 아닌지 확인한다. (call 상태인지 확인)
6. 배차 완료된 콜이면 "배차완료된 콜입니다" 메세지를 보낸다.
7. 배차 전인 콜이면 배차된다.
8. 고객에게 콜이 배차되었다고 상태를 업데이트한다.


[ 비기능적 요구사항 ]
1. 트랜잭션
    1. 배차가 가능한 콜만 수락할 수 있다. Sync 호출 
1. 장애격리
    1. call 서비스가 중단되더라도 고객은 365일 24시간 call을 부를 수 있어야 한다  Async (event-driven), Eventual Consistency
    2. 콜이 과도하게 들어와서 call 서비스가 과중되더라도 콜 정보를 call 서비스가 정상화 된 이후에 수신한다 Circuit breaker, fallback
1. 성능
    1. 주문 접수 상태가 바뀔때마다 고객에게 알림을 줄 수 있어야 한다  Event driven


# 분석/설계

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  
![image](https://user-images.githubusercontent.com/24731820/120582100-e30e3700-c466-11eb-825f-0748bc1031fc.png)

![image](https://user-images.githubusercontent.com/24731820/120582568-9d9e3980-c467-11eb-8cf7-772a4b2c91d0.png)

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
        - 콜상태조회, 택시상태조회, 배차여부조회
          :  UI 의 이벤트이지, 업무적인 의미의 이벤트가 아니라서 제외

### 바운디드 컨텍스트

![image](https://user-images.githubusercontent.com/24731820/120582800-13a2a080-c468-11eb-9ef4-9181825cc1d7.png)

    - Core Domain:  Passenger, Call, Taxi : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표

### 완성된 1차 모형

![image](https://user-images.githubusercontent.com/24731820/120582100-e30e3700-c466-11eb-825f-0748bc1031fc.png)


### 기능적 요구사항을 커버하는지 검증

![image](https://user-images.githubusercontent.com/24731820/120583220-c2df7780-c468-11eb-99de-a5124fd82134.png)

    - 승객이 택시를 호출한다. (OK)
    - 콜 목록에 고객의 콜이 쌓인다. (OK)
    - 택시 기사에게 콜이 전송된다. (OK)
    -  택시 기사가 콜을 승락한다. (OK)
    - 콜 목록에서 해당 콜이 배차완료 콜이 아닌지 확인한다. (OK)
    - 배차 완료된 콜이면 "배차완료된 콜입니다" 메세지를 보낸다. (OK)
    - 배차 전인 콜이면 배차된다. (OK)
    - 고객에게 콜이 배차되었다고 상태를 업데이트한다. (OK)

### 비기능 요구사항에 대한 검증

![image](https://user-images.githubusercontent.com/24731820/120583503-52852600-c469-11eb-892b-465c2d59fac2.png)

    1. 트랜잭션 (OK)
      1. 배차가 가능한 콜만 수락할 수 있다. Sync 호출 
    2. 장애격리 (OK)
      1. call 서비스가 중단되더라도 고객은 365일 24시간 call을 부를 수 있어야 한다  Async (event-driven), Eventual Consistency
      2. 콜이 과도하게 들어와서 call 서비스가 과중되더라도 콜 정보를 call 서비스가 정상화 된 이후에 수신한다 Circuit breaker, fallback
    3.성능 (OK)
     1. 주문 접수 상태가 바뀔때마다 고객에게 알림을 줄 수 있어야 한다  Event driven


## 헥사고날 아키텍처 다이어그램 도출

![image](https://user-images.githubusercontent.com/24731820/120593547-a1878700-c47a-11eb-937e-6533858f3c3f.png)

    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8083 이다)

```
cd taxi
mvn spring-boot:run

cd passenger
mvn spring-boot:run 

cd call
mvn spring-boot:run  
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다.
```
@Entity
@Table(name = "Passenger_table")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long passengerId;
    private String startLocation;
    private String endLocation;
    private String status;

    @PostPersist
    public void onPostPersist() {
        CalledTaxi calledtaxi = new CalledTaxi();
        BeanUtils.copyProperties(this, calledtaxi);
        calledtaxi.publishAfterCommit();

    }

    public Long getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(Long passengerId) {
        this.passengerId = passengerId;
    }

    public String getStartLocation() {
        return startLocation;
    }

    public void setStartLocation(String startLocation) {
        this.startLocation = startLocation;
    }

    public String getEndLocation() {
        return endLocation;
    }

    public void setEndLocation(String endLocation) {
        this.endLocation = endLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}

```
- Entity Pattern 과 Repository Pattern 을 적용하여 JPA 를 통하여 다양한 데이터소스 유형 (RDB or NoSQL) 에 대한 별도의 처리가 없도록 데이터 접근 어댑터를 자동 생성하기 위하여 Spring Data REST 의 RestRepository 를 적용하였다
```
@RepositoryRestResource(collectionResourceRel = "passengers", path = "passengers")
public interface PassengerRepository extends PagingAndSortingRepository<Passenger, String> {

}
```
- 적용 후 REST API 의 테스트
- 
```
# 택시 요청 
http POST http://localhost:8082/passengers startLocation=서울역 endLocation=강남역 status=call

# 콜 승락
http POST http://localhost:8081/taxis/accept callId=6 startLocation=서울역 endLocation=강남역 status=accept

# 콜 상태 확인
http GET http://localhost:8082/passengers
```

## 폴리글랏 퍼시스턴스

택시(taxi) 는 h2 db가 아닌 apache derby를 사용하기로 하였다. h2와 유사한 embedded db이기 때문에 pom.xml에 dependency만 추가/삭제하여 derby 를 부착시켰다

```
# pom.xml
(h2 dependency 제거 후, derby dependency 추가)
<!-- apache derby -->
<dependency>
	<groupId>org.apache.derby</groupId>
	<artifactId>derby</artifactId>
	<scope>runtime</scope>
</dependency>

```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 택시(taxi)->콜(call) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 콜 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
@FeignClient(name = "call", url = "${feign.client.url.callUrl}")
public interface CallService {

    @PutMapping("/calls/{id}")
    public boolean updateCall(@PathVariable Long id, @RequestBody Map<String, Object> payload);

}
```

- 배차 요청 받은 즉시 해당 콜을 배차 상태로 변경
```
  @PutMapping("/{id}")
    public boolean updateCall(@PathVariable Long id, @RequestBody Map<String, Object> payload) throws Exception {
        Call call = callRepository.findById(id).get();

        if (call.getStatus().equals("call")) {
            call.setStatus(payload.get("status").toString());
            callRepository.save(call);

            return true;
        } else {
            return false;
        }
    }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 콜 시스템이 장애가 나면 배차 요 못받는다는 것을 확인:


```
# 콜 (call) 서비스를 잠시 내려놓음 (ctrl+c, replicas 0 으로 설정)

# 콜 승락
http POST http://localhost:8081/taxis/accept callId=6 startLocation=서울역 endLocation=강남역 status=accept # fail
http POST http://localhost:8081/taxis/accept callId=6 startLocation=서울역 endLocation=강남역 status=accept # fail


# 콜 서비스 재기동
cd call
mvn spring-boot:run

# 콜 승락
http POST http://localhost:8081/taxis/accept callId=6 startLocation=서울역 endLocation=강남역 status=accept # success
http POST http://localhost:8081/taxis/accept callId=6 startLocation=서울역 endLocation=강남역 status=accept # success

```

## 비동기식 호출 publish-subscribe

고객이 택시 요청을 한 후, 콜 시스템에게 이를 알려주는 행위는 동기식이 아닌 비동기식으로 처리한다.
- 이를 위하여 택시 요청 접수된 후에 곧바로 콜이 접수 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
@Entity
@Table(name = "Passenger_table")
public class Passenger {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long passengerId;
    private String startLocation;
    private String endLocation;
    private String status;

    @PostPersist
    public void onPostPersist() {
        CalledTaxi calledtaxi = new CalledTaxi();
        BeanUtils.copyProperties(this, calledtaxi);
        calledtaxi.publishAfterCommit();

    }
}
```
- 콜 서비스에서는 택시 접수 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
@Service
public class PolicyHandler {
    @Autowired
    CallRepository callRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCalledTaxi_CreateCall(@Payload CalledTaxi calledTaxi) {

        if (!calledTaxi.validate())
            return;

        System.out.println("\n\n##### listener CreateCall : " + calledTaxi.toJson() + "\n\n");

        Call call = new Call();
        BeanUtils.copyProperties(calledTaxi, call);
        callRepository.save(call);
    }
}

```

배송 시스템은 주문 시스템과 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 배송시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
# 배송 서비스 (delivery) 를 잠시 내려놓음 

# 택시 요청 
# success
http POST http://localhost:8082/passengers startLocation=서울역 endLocation=강남역 status=call

#  서비스 기동
cd delivery
mvn spring-boot:run

# 콜 목록 확인
# call이 수신됨을 확인
http GET http://localhost:8083/calls     
```


# 운영

## CICD 설정
Taxi의 ECR 구성은 아래와 같다.
![image](https://user-images.githubusercontent.com/24731820/120596968-63409680-c47f-11eb-9ef6-cd6339cf3230.png)


## Kubernetes 설정
AWS EKS를 활용했으며, 추가한 namespace는 coffee와 kafka로 아래와 같다.

###EKS Deployment

namespace: taxi
![image](https://user-images.githubusercontent.com/24731820/120605516-18c41780-c489-11eb-9f69-a58e69ccd334.png)

namespace: kafka
![image](https://user-images.githubusercontent.com/24731820/120605559-24afd980-c489-11eb-8809-b66135b81918.png)

###EKS Service
gateway가 아래와 같이 LoadBalnacer 역할을 수행한다  

```
➜  ~  kubectl get service -o wide -n taxi
NAME        TYPE           CLUSTER-IP       EXTERNAL-IP                                                                   PORT(S)          AGE   SELECTOR
call        ClusterIP      10.100.114.137   <none>                                                                        8080/TCP         23m   app=call
gateway     LoadBalancer   10.100.43.60     ac62074d7fa99475b822702e04b903b0-595332432.ap-southeast-1.elb.amazonaws.com   8080:31105/TCP   17m   app.kubernetes.io/name=gateway
passenger   ClusterIP      10.100.16.100    <none>                                                                        8080/TCP         23m   app=passenger
taxi        ClusterIP      10.100.28.150    <none>                                                                        8080/TCP         23m   app=taxi
```


## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 택시(taxi)-->콜(call) 연결을 RestFul Request/Response 로 연동하여 구현이 되어있고, 배차 요청이 과도할 경우 CB 를 통하여 장애격리.

- Hystrix 를 설정:  요청처리 쓰레드에서 처리시간이 610 밀리가 넘어서기 시작하여 어느정도 유지되면 CB 회로가 닫히도록 (요청을 빠르게 실패처리, 차단) 설정
```
# application.yml
feign:
  hystrix:
    enabled: true
    
hystrix:
  command:
    # 전역설정
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610

```
- (call) 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
      @PutMapping("/{id}")
    public boolean updateCall(@PathVariable Long id, @RequestBody Map<String, Object> payload) throws Exception {
        
        try {
               Thread.currentThread().sleep((long) (400 + Math.random() * 220));
        } catch (InterruptedException e) {
               e.printStackTrace();
        }

        return true;
        
     } 
                
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 15명
- 60초 동안 실시

```
siege -c15 -t60S --content-type "application/json" 'http://ac62074d7fa99475b822702e04b903b0-595332432.ap-southeast-1.elb.amazonaws.com:8080/taxis/accept POST {"callId":1, "startLocation": "서울역", "endLocation": "강남역", "status" : "accept" }'

Transactions:                    863 hits
Availability:                  45.59 %
Elapsed time:                  51.38 secs
Data transferred:               0.28 MB
Response time:                  0.89 secs
Transaction rate:              16.80 trans/sec
Throughput:                     0.01 MB/sec
Concurrency:                   14.93
Successful transactions:         863
Failed transactions:            1030
Longest transaction:            0.79
Shortest transaction:           0.14
```
- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호. 
  시스템의 안정적인 운영을 위해 HPA 적용 필요.



### Autoscale HPA

- 택시서비스에 대해 HPA를 설정한다. 설정은 CPU 사용량이 5%를 넘어서면 pod를 5개까지 추가한다.
```
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: taxi
  namespace: taxi
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: order
  minReplicas: 1
  maxReplicas: 5
  targetCPUUtilizationPercentage: 5

➜  ~ kubectl get hpa -n taxi
NAME        REFERENCE              TARGETS   MINPODS   MAXPODS   REPLICAS   AGE
call        Deployment/call        5%/5%     1         5         5          14h
gateway     Deployment/gateway     8%/5%     1         5         5          14h
passenger   Deployment/passenger   121%/5%   1         5         5          14h
taxi        Deployment/taxi        6%/5%     1         5         5          14h
```
- 부하를 1분간 유지한다.
```
➜  ~ siege -c15 -t60S --content-type "application/json" 'http://ac62074d7fa99475b822702e04b903b0-595332432.ap-southeast-1.elb.amazonaws.com:8080/taxis/accept POST {"callId":1, "startLocation": "서울역", "endLocation": "강남역", "status" : "accept" }'
```
- 오토스케일이 어떻게 되고 있는지 확인한다.
```
➜  ~ kubectl get deploy -n taxi
NAME        READY   UP-TO-DATE   AVAILABLE   AGE
call        1/5     5            1           15h
gateway     1/5     5            5           15h
passenger   1/5     5            1           15h
taxi        1/5     5            5           15h
```
- 어느정도 시간이 흐르면 스케일 아웃이 동작하는 것을 확인
```
➜  ~ kubectl get deploy -n taxi
NAME        READY   UP-TO-DATE   AVAILABLE   AGE
call        1/5     5            1           15h
gateway     5/5     5            5           15h
passenger   1/5     5            1           15h
taxi        5/5     5            5           15h
```

- Availability 가 높아진 것을 확인 (siege)
```
Transactions:                   1163 hits
Availability:                  89.26 %
Elapsed time:                  59.34 secs
Data transferred:               0.12 MB
Response time:                  0.76 secs
Transaction rate:              19.60 trans/sec
Throughput:                     0.00 MB/sec
Concurrency:                   14.91
Successful transactions:        1163
Failed transactions:             140
Longest transaction:            0.96
Shortest transaction:           0.16
```


## ConfigMap 설정
특정값을 k8s 설정으로 올리고 서비스를 기동 후, kafka 정상 접근 여부 확인한다.
```
kubectl describe cm taxi -n taxi
Name:         taxi
Namespace:    taxi
Labels:       <none>
Annotations:  <none>

Data
====
taxiKafka:
----
my-kafka.kafka.svc.cluster.local:9092
Events:  <none>
```
관련된 application.yml 파일 설정은 다음과 같다. 
```
    spring:
      profiles: docker
      cloud:
        stream:
          kafka:
            binder:
              brokers: ${taxiKafka}
```
EKS 설치된 kafka에 정상 접근된 것을 확인할 수 있다. (해당 configMap TEXT1 값을 잘못된 값으로 넣으면 kafka WARN)
```
2021-06-03 05:28:24.510 INFO 1 --- [pool-1-thread-1] o.a.kafka.common.utils.AppInfoParser : Kafka version : 2.0.1
2021-06-03 05:28:24.510 INFO 1 --- [pool-1-thread-1] o.a.kafka.common.utils.AppInfoParser : Kafka commitId : fa14705e51bd2ce5
2021-06-03 05:28:24.519 INFO 1 --- [pool-1-thread-1] org.apache.kafka.clients.Metadata : Cluster ID: _gCXCLQSQ0mXXI993jUh0Q
2021-06-03 05:28:26.780 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator : [Consumer clientId=consumer-3, groupId=taxi] Attempt to heartbeat failed since group is rebalancing
2021-06-03 05:28:26.780 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator : [Consumer clientId=consumer-3, groupId=taxi] Revoking previously assigned partitions []
2021-06-03 05:28:26.780 INFO 1 --- [container-0-C-1] o.s.c.s.b.k.KafkaMessageChannelBinder$1 : partitions revoked: []
2021-06-03 05:28:26.781 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator : [Consumer clientId=consumer-3, groupId=taxi] (Re-)joining group
2021-06-03 05:28:26.789 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator : [Consumer clientId=consumer-3, groupId=taxi] Successfully joined group with generation 10
2021-06-03 05:28:26.790 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator : [Consumer clientId=consumer-3, groupId=taxi] Setting newly assigned partitions [taxi-0]
2021-06-03 05:28:26.801 INFO 1 --- [container-0-C-1] o.s.c.s.b.k.KafkaMessageChannelBinder$1 : partitions assigned: [taxi-0]
2021-06-03 05:29:11.375 INFO 1 --- [nio-8080-exec-1] o.h.h.i.QueryTranslatorFactoryInitiator : HHH000397: Using ASTQueryTranslatorFactory
```

## Zero-downtime deploy
k8s의 무중단 서비스 배포 기능을 점검한다.
```
    ➜  ~ kubectl describe deploy taxi -n taxi
Name:                   taxi
Namespace:              taxi
CreationTimestamp:      Thu, 03 Jun 2021 15:25:24 +0900
Labels:                 app=taxi
Annotations:            deployment.kubernetes.io/revision: 3
Selector:               app=taxi
Replicas:               1 desired | 1 updated | 1 total | 1 available | 0 unavailable
StrategyType:           RollingUpdate
MinReadySeconds:        0
RollingUpdateStrategy:  25% max unavailable, 25% max surge
Pod Template:
  Labels:  app=taxi
  Containers:
   taxi:
    Image:      879772956301.dkr.ecr.ap-southeast-1.amazonaws.com/user09-taxi:v7
    Port:       8080/TCP
    Host Port:  0/TCP
    Liveness:   http-get http://:8080/actuator/health delay=120s timeout=2s period=5s #success=1 #failure=5
    Readiness:  http-get http://:8080/actuator/health delay=10s timeout=2s period=5s #success=1 #failure=10
    Environment Variables from:
      taxi        ConfigMap  Optional: false
    Environment:  <none>
    Mounts:       <none>
  Volumes:        <none>
Conditions:
  Type           Status  Reason
  ----           ------  ------
  Available      True    MinimumReplicasAvailable
  Progressing    True    NewReplicaSetAvailable
OldReplicaSets:  <none>
NewReplicaSet:   taxi-567688979f (1/1 replicas created)
Events:
  Type    Reason             Age   From                   Message
  ----    ------             ----  ----                   -------
  Normal  ScalingReplicaSet  32m   deployment-controller  Scaled up replica set taxi-567688979f to 1
  Normal  ScalingReplicaSet  31m   deployment-controller  Scaled down replica set taxi-7c54b9966d to 0
```
replicas을 4로 설정 후,
위 Readiness와 RollingUpdateStrategy 설정이 정상 적용되는지 확인한다.
```
    ➜  ~ kubectl apply -n taxi -f C:\DEV\Workspace\taxi\taxi\kubernetes\deployment.yml

    ➜  ~  kubectl get po -n taxi
    NAME                         READY   STATUS    RESTARTS   AGE
    call-89dcf55dc-qcln6         1/1     Running   0          153m
    gateway-fc944b68b-dnsfd      1/1     Running   0          152m
    passenger-7c977c8f6d-rk8dz   1/1     Running   0          153m
    taxi-6747ddf654-5nfbh        1/1     Running   0          131m
    taxi-6747ddf654-dv5hn        1/1     Running   0          133m
    taxi-6747ddf654-kc5d6        1/1     Running   0          133m
    taxi-6747ddf654-rgtsp        1/1     Running   0          131m
    taxi-6747ddf654-tcjfs        0/1     Running   0          61s
    
    ➜  ~ kubectl get po -n taxi
    NAME                         READY   STATUS    RESTARTS   AGE
    call-89dcf55dc-qcln6         1/1     Running   0          167m
    gateway-fc944b68b-dnsfd      1/1     Running   0          166m
    passenger-7c977c8f6d-rk8dz   1/1     Running   0          167m
    taxi-6747ddf654-5nfbh        1/1     Running   0          145m
    taxi-6747ddf654-dv5hn        1/1     Running   0          147m
    taxi-6747ddf654-hs6x2        0/1     Running   0          19s
    taxi-6747ddf654-rgtsp        1/1     Running   0          145m
    taxi-694b9897cf-62s4f        0/1     Running   0          34s
    taxi-694b9897cf-8nms7        0/1     Running   0          34s
    taxi-694b9897cf-kx4cf        0/1     Running   0          19s
    
    ➜  ~ kubectl get po -n taxi
    NAME                         READY   STATUS    RESTARTS   AGE
    call-89dcf55dc-qcln6         1/1     Running   0          171m
    gateway-fc944b68b-dnsfd      1/1     Running   0          169m
    passenger-7c977c8f6d-rk8dz   1/1     Running   0          170m
    taxi-694b9897cf-62s4f        1/1     Running   0          4m8s
    taxi-694b9897cf-6v8nw        1/1     Running   0          2m26s
    taxi-694b9897cf-8nms7        1/1     Running   0          4m8s
    taxi-694b9897cf-kx4cf        1/1     Running   0          3m53s
    taxi-694b9897cf-qrztv        1/1     Running   0          2m21s
```
배포시 pod는 위의 흐름과 같이 생성 및 종료되어 서비스의 무중단을 보장했다.


## 셀프힐링 (livenessProbe 설정)
- order deployment livenessProbe 
```
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
              scheme: HTTP
            initialDelaySeconds: 120
            timeoutSeconds: 2
            periodSeconds: 5
            successThreshold: 1
            failureThreshold: 5
```
livenessProbe 기능 점검을 위해 HPA 제거한다.
```
➜  ~ kubectl get hpa -n coffee
No resources found in coffee namespace.
```
Pod 의 변화를 살펴보기 위하여 watch
```
➜  ~ kubectl get -n coffee po -w
NAME                        READY   STATUS    RESTARTS   AGE
customer-785f544f95-mh456   1/1     Running   0          23h
delivery-557f4d7f49-z47bx   1/1     Running   0          23h
gateway-6886bbf85b-4hggj    1/1     Running   0          149m
gateway-6886bbf85b-mg9fz    1/1     Running   0          22h
order-659cd7bddf-glgjj      1/1     Running   0          22m
product-7c5c949965-z6pqs    1/1     Running   0          131m
report-85dd84c856-qbzbc     1/1     Running   0          16h
```
order 서비스를 다운시키기 위한 부하 발생
```
➜  ~ siege -c50 -t60S -r10 --content-type "application/json" 'http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders POST {"customerId":2, "productId":1}'
```
order Pod의 liveness 조건 미충족에 의한 RESTARTS 횟수 증가 확인
```
➜  ~ kubectl get -n coffee po -w
NAME                        READY   STATUS    RESTARTS   AGE
customer-785f544f95-mh456   1/1     Running   0          23h
delivery-557f4d7f49-z47bx   1/1     Running   0          23h
gateway-6886bbf85b-4hggj    1/1     Running   0          157m
gateway-6886bbf85b-mg9fz    1/1     Running   0          22h
order-659cd7bddf-glgjj      1/1     Running   1          30m
product-7c5c949965-z6pqs    1/1     Running   0          138m
report-85dd84c856-qbzbc     1/1     Running   0          16h
```
