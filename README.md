# 1조 프로젝트 : SirenOrder

![image](https://user-images.githubusercontent.com/74900977/118920002-81cb6b80-b970-11eb-8ca7-a5e62d96a77e.png)

SirenOrder 서비스를 MSA/DDD/Event Storming/EDA 를 포괄하는 분석/설계/구현/운영 전단계를 커버하도록 구성한 프로젝트임

- 체크포인트 : https://workflowy.com/s/assessment/qJn45fBdVZn4atl3


# Table of contents

- [예제 - SirenOrder](#---)
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
1. 고객이 회원 가입을 한다
2. 신규 회원 가입을 한 고객에게 포인트를 적립해 준다
3. 고객이 주문하기 전에 주문 가능한 상품 메뉴를 선택한다
4. 고객이 선택한 메뉴에 대해서 주문을 한다
5. 주문이 되면 주문 내역이 Delivery 서비스에 전달되고, 고객 포인트를 적립한다
6. 접수된 주문은 Wating 상태로 접수가 되고, 고객한테 접수 대기 번호를 발송한다
7. 주문한 상품이 완료되면 고객한테 상품 주문 완료를 전달한다
8. 상점 주인에게 주문/매출 정보를 조회할수 있는 Report 서비스를 제공한다.


[ 비기능적 요구사항 ]
1. 트랜잭션
    1. 판매가 가능한 상품 정보만 주문 메뉴에 노출한다  Sync 호출 
1. 장애격리
    1. Delivery 서비스가 중단되더라도 주문은 365일 24시간 받을 수 있어야 한다  Async (event-driven), Eventual Consistency
    1. 주문이 완료된 상품이 Delivery 서비스가 과중되더라도 주문 완료 정보를 Delivery 서비스가 정상화 된 이후에 수신한다 Circuit breaker, fallback
1. 성능
    1. 상점 주인은 Report 서비스를 통해서 주문/매출 정보를 확인할 수 있어야 한다  CQRS
    1. 주문 접수 상태가 바뀔때마다 고객에게 알림을 줄 수 있어야 한다  Event driven


# 분석/설계

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/f2NszwGXcITtKN4MrX4BrDurru12/share/99c158ed8a4d29f04a25679ea2240382


### 이벤트 도출
![image](https://user-images.githubusercontent.com/74900977/118924080-8ba49d00-b977-11eb-82f2-4db4f4be71fa.png)

### 부적격 이벤트 탈락
![image](https://user-images.githubusercontent.com/74900977/118924474-1ab1b500-b978-11eb-8dd3-9fcd7a003a13.png)

    - 과정중 도출된 잘못된 도메인 이벤트들을 걸러내는 작업을 수행함
        - 주문시>메뉴카테고리선택됨, 주문시>메뉴검색됨, 주문후>포인트 조회함, 주문후>주문 상태값 조회됨
          :  UI 의 이벤트이지, 업무적인 의미의 이벤트가 아니라서 제외

### 바운디드 컨텍스트

![image](https://user-images.githubusercontent.com/74900977/118925812-4df54380-b97a-11eb-9591-a924fe52e9e0.png)

    - 도메인 서열 분리 
        - Core Domain:  Customer, Order, Product, Delivery : 없어서는 안될 핵심 서비스이며, 연견 Up-time SLA 수준을 99.999% 목표, 배포주기 : 1주일 1회 미만, Delivery 1개월 1회 미만
        - Supporting Domain: Report : 경쟁력을 내기위한 서비스이며, SLA 수준은 연간 60% 이상 uptime 목표, 배포주기 : 1주일 1회 이상을 기준 ( 각팀 배포 주기 Policy 적용 )

### 완성된 1차 모형

![image](https://user-images.githubusercontent.com/74900977/118931820-581b4000-b982-11eb-963a-a47b5f014844.png)


### 기능적 요구사항을 커버하는지 검증

![image](https://user-images.githubusercontent.com/74900977/118940019-425e4880-b98b-11eb-85ce-16375ba40f1e.png)

    - 고객이 회원 가입을 한다 (ok)
    - 신규 회원 가입을 한 고객에게 포인트를 적립해 준다(OK)
    - 고객이 주문하기 전에 주문 가능한 상품 메뉴를 선택한다 (ok)
    - 고객이 선택한 메뉴에 대해서 주문을 한다 (ok)
    - 주문이 되면 주문 내역이 Delivery 서비스에 전달되고, 고객 포인트를 적립한다 (ok)
    - 접수된 주문은 Wating 상태로 접수가 되고, 고객한테 접수 대기 번호를 발송한다 ( ok )
    - 주문한 상품이 완료되면 고객한테 상품 주문 완료를 전달한다 ( OK )
    - 상점 주인에게 주문/매출 정보를 조회할수 있는 Report 서비스를 제공한다 ( OK )


### 비기능 요구사항에 대한 검증

![image](https://user-images.githubusercontent.com/74900977/118941404-a6cdd780-b98c-11eb-9d26-a17a83a5c9ee.png)

    - 마이크로 서비스를 넘나드는 시나리오에 대한 트랜잭션 처리
    - 판매 가능 상품 :  판매가 가능한 상품만 주문 메뉴에 노출됨 , ACID 트랜잭션, Request-Response 방식 처리
    - 주문 완료시 상품 접수 및 Delivery:  Order 서비스에서 Delivery 마이크로서비스로 주문요청이 전달되는 과정에 있어서 Delivery 마이크로 서비스가 별도의 배포주기를 가지기 때문에 Eventual Consistency 방식으로 트랜잭션 처리함.
    - Product, Customer, Report MicroService 트랜잭션:  주문 접수 상태, 상품 준비 상태 등 모든 이벤트에 대해 Kafka를 통한 Async 방식 처리, 데이터 일관성의 시점이 크리티컬하지 않은 모든 경우가 대부분이라 판단, Eventual Consistency 를 기본으로 채택함.




## 헥사고날 아키텍처 다이어그램 도출

![image](https://user-images.githubusercontent.com/74900977/118951124-c9182300-b995-11eb-8b4d-9107d3dcf501.png)

    - Chris Richardson, MSA Patterns 참고하여 Inbound adaptor와 Outbound adaptor를 구분함
    - 호출관계에서 PubSub 과 Req/Resp 를 구분함
    - 서브 도메인과 바운디드 컨텍스트의 분리:  각 팀의 KPI 별로 아래와 같이 관심 구현 스토리를 나눠가짐


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8085 이다)

```
cd customer
mvn spring-boot:run

cd order
mvn spring-boot:run 

cd product
mvn spring-boot:run  

cd delivery
mvn spring-boot:run  

cd report
mvn spring-boot:run  
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다.
```
package coffee;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@Table(name = "Delivery_table")
public class Delivery {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private Long orderId;
    private String status;

    @PostPersist
    public void onPostPersist() {
        OrderWaited orderWaited = new OrderWaited();
        BeanUtils.copyProperties(this, orderWaited);
        orderWaited.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate() {
        StatusUpdated statusUpdated = new StatusUpdated();
        BeanUtils.copyProperties(this, statusUpdated);
        statusUpdated.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
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
package coffee;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface OrderRepository extends PagingAndSortingRepository<Order, Long> {
    public int countByStatus(String status);
}
```
- 적용 후 REST API 의 테스트
```
# 주문 처리
http POST http://localhost:8082/orders customerId=100 productId=100
http POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders customerId=100 productId=100

# 배달 완료 처리
http PATCH http://localhost:8084/deliveries/1 status=Completed
http PATCH http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/deliveries/1 status=Completed

# 주문 상태 확인
http GET http://localhost:8082/orders/1
http GET http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders/1
```

## 동기식 호출 과 Fallback 처리

분석단계에서의 조건 중 하나로 주문(order)->고객(customer) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 호출 프로토콜은 이미 앞서 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다. 

- 고객 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현 

```
package coffee.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "customer", url = "${feign.client.url.customerUrl}")
public interface CustomerService {

    @RequestMapping(method = RequestMethod.GET, path = "/customers/checkAndModifyPoint")
    public boolean checkAndModifyPoint(@RequestParam("customerId") Long customerId,
            @RequestParam("price") Integer price);

}
```

- 주문 받은 즉시 고객 포인트를 차감하도록 구현
```
@RequestMapping(value = "/checkAndModifyPoint", method = RequestMethod.GET)
  public boolean checkAndModifyPoint(@RequestParam("customerId") Long customerId, @RequestParam("price") Integer price) throws Exception {
          System.out.println("##### /customer/checkAndModifyPoint  called #####");

          boolean result = false;

          Optional<Customer> customerOptional = customerRepository.findById(customerId);
          Customer customer = customerOptional.get();
          if (customer.getCustomerPoint() >= price) {
                  result = true;
                  customer.setCustomerPoint(customer.getCustomerPoint() - price);
                  customerRepository.save(customer);
          }

          return result;
  }
```

- 동기식 호출에서는 호출 시간에 따른 타임 커플링이 발생하며, 고객 시스템이 장애가 나면 주문도 못받는다는 것을 확인:


```
# 고객 (customer) 서비스를 잠시 내려놓음 (ctrl+c, replicas 0 으로 설정)

#주문처리 
http POST http://localhost:8082/orders customerId=100 productId=100   #Fail
http POST http://localhost:8082/orders customerId=101 productId=101   #Fail

#고객서비스 재기동
cd 결제
mvn spring-boot:run

#주문처리
http POST http://localhost:8082/orders customerId=100 productId=100   #Success
http POST http://localhost:8082/orders customerId=101 productId=101   #Success
```



## 비동기식 호출 publish-subscribe

주문이 완료된 후, 배송 시스템에게 이를 알려주는 행위는 동기식이 아닌 비동기식으로 처리한다.
- 이를 위하여 주문이 접수된 후에 곧바로 주문 접수 되었다는 도메인 이벤트를 카프카로 송출한다(Publish)
 
```
package coffee;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;

@Entity
@DynamicInsert
@Table(name = "Order_table")
public class Order {

 ...
     @PostPersist
    public void onPostPersist() throws Exception {

        Integer price = OrderApplication.applicationContext.getBean(coffee.external.ProductService.class)
                .checkProductStatus(this.getProductId());

        if (price > 0) {
            boolean result = OrderApplication.applicationContext.getBean(coffee.external.CustomerService.class)
                    .checkAndModifyPoint(this.getCustomerId(), price);

            if (result) {

                Ordered ordered = new Ordered();
                BeanUtils.copyProperties(this, ordered);
                ordered.publishAfterCommit();

            } else
                throw new Exception("Customer Point - Exception Raised");
        } else
            throw new Exception("Product Sold Out - Exception Raised");
    }

}
```
- 배송 서비스에서는 주문 상태 접수 이벤트에 대해서 이를 수신하여 자신의 정책을 처리하도록 PolicyHandler 를 구현한다:

```
package coffee;
...

@Service
public class PolicyHandler {

    @Autowired
    DeliveryRepository deliveryRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrdered_WaitOrder(@Payload Ordered ordered) {

        if (ordered.isMe()) {
            System.out.println("##### listener WaitOrder : " + ordered.toJson());

            Delivery delivery = new Delivery();
            delivery.setOrderId(ordered.getId());
            delivery.setStatus("Waited");

            deliveryRepository.save(delivery);

        }
    }

}

```

배송 시스템은 주문 시스템과 완전히 분리되어있으며, 이벤트 수신에 따라 처리되기 때문에, 배송시스템이 유지보수로 인해 잠시 내려간 상태라도 주문을 받는데 문제가 없다:
```
# 배송 서비스 (delivery) 를 잠시 내려놓음 (ctrl+c)

#주문처리
http POST http://localhost:8082/orders customerId=100 productId=100   #Success

#주문상태 확인
http GET http://localhost:8082/orders/1     # 주문상태 Ordered 확인

#배송 서비스 기동
cd delivery
mvn spring-boot:run

#주문상태 확인
http GET localhost:8082/orders/1     # 주문 상태 Waited로 변경 확인
```


# 운영

## CICD 설정
SirenOrder의 ECR 구성은 아래와 같다.
![image](https://user-images.githubusercontent.com/20352446/118971683-ad6b4780-b9aa-11eb-893a-1cd05a95ea11.png)

사용한 CI/CD 도구는 AWS CodeBuild
![image](https://user-images.githubusercontent.com/20352446/118972243-4d28d580-b9ab-11eb-83aa-5cd39d06a784.png)
GitHub Webhook이 동작하여 Docker image가 자동 생성 및 ECR 업로드 된다.
(pipeline build script 는 report 폴더 이하에 buildspec.yaml 에 포함)
![image](https://user-images.githubusercontent.com/20352446/118972320-6467c300-b9ab-11eb-811a-423bcb9b59e2.png)
참고로 그룹미션 작업의 편의를 위해 하나의 git repository를 사용하였다


## Kubernetes 설정
AWS EKS를 활용했으며, 추가한 namespace는 coffee와 kafka로 아래와 같다.

###EKS Deployment

namespace: coffee
![image](https://user-images.githubusercontent.com/20352446/118971846-d986c880-b9aa-11eb-8872-5baf9083d99a.png)

namespace: kafka
![image](https://user-images.githubusercontent.com/20352446/118973352-8dd51e80-b9ac-11eb-8d5f-ac6aa9fe9e5a.png)

###EKS Service
gateway가 아래와 같이 LoadBalnacer 역할을 수행한다  

    ➜  ~ kubectl get service -o wide -n coffee
    NAME       TYPE           CLUSTER-IP       EXTERNAL-IP                                                                    PORT(S)          AGE     SELECTOR
    customer   ClusterIP      10.100.166.116   <none>                                                                         8080/TCP         8h      app=customer
    delivery   ClusterIP      10.100.138.255   <none>                                                                         8080/TCP         8h      app=delivery
    gateway    LoadBalancer   10.100.59.190    ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com   8080:31716/TCP   6h11m   app=gateway
    order      ClusterIP      10.100.123.133   <none>                                                                         8080/TCP         8h      app=order
    product    ClusterIP      10.100.170.95    <none>                                                                         8080/TCP         5h44m   app=product
    report     ClusterIP      10.100.127.177   <none>                                                                         8080/TCP         4h41m   app=report


## 동기식 호출 / 서킷 브레이킹 / 장애격리

* 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 옵션을 사용하여 구현함

시나리오는 주문(order)-->상품(product) 연결을 RestFul Request/Response 로 연동하여 구현이 되어있고, 주문이 과도할 경우 CB 를 통하여 장애격리.

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
- 상품(product) 임의 부하 처리 - 400 밀리에서 증감 220 밀리 정도 왔다갔다 하게
```
        @RequestMapping(value = "/products/checkProductStatus", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")
        public Integer checkProductStatus(@RequestParam("productId") Long productId) throws Exception {
                
                //FIXME 생략
                
                //임의의 부하를 위한 강제 설정
                try {
                        Thread.currentThread().sleep((long) (400 + Math.random() * 220));
                } catch (InterruptedException e) {
                        e.printStackTrace();
                }

                return price;
        }
```

* 부하테스터 siege 툴을 통한 서킷 브레이커 동작 확인:
- 동시사용자 100명
- 60초 동안 실시

```
siege -c100 -t60S -r10 --content-type "application/json" 'http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders POST {"customerId":2, "productId":3}'

HTTP/1.1 201     6.51 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     0.73 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.03 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.22 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.25 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.20 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.24 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.31 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.29 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.42 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.23 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.30 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201    11.88 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     0.66 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.29 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.41 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders
HTTP/1.1 201     6.33 secs:     239 bytes ==> POST http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders

Transactions:		         659 hits
Availability:		       36.98 %
Elapsed time:		       58.42 secs
Data transferred:	        0.98 MB
Response time:		        8.59 secs
Transaction rate:	       11.28 trans/sec
Throughput:		        0.02 MB/sec
Concurrency:		       96.94
Successful transactions:         659
Failed transactions:	        1123
Longest transaction:	       27.38
Shortest transaction:	        0.01

```
- 운영시스템은 죽지 않고 지속적으로 CB 에 의하여 적절히 회로가 열림과 닫힘이 벌어지면서 자원을 보호. 
  시스템의 안정적인 운영을 위해 HPA 적용 필요.



### Autoscale HPA

- 주문서비스에 대해 HPA를 설정한다. 설정은 CPU 사용량이 5%를 넘어서면 pod를 5개까지 추가한다.(memory 자원 이슈로 10개 불가)
```
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
  name: product
  namespace: coffee
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: product
  minReplicas: 1
  maxReplicas: 5
  targetCPUUtilizationPercentage: 5

➜  ~ kubectl get hpa -n coffee
NAME      REFERENCE            TARGETS         MINPODS   MAXPODS   REPLICAS   AGE
order     Deployment/order     30%/5%          1         5         5          17h
product   Deployment/product   31%/10%         1         5         5          132m
```
- 부하를 2분간 유지한다.
```
➜  ~ siege -c30 -t60S -r10 --content-type "application/json" 'http://ac4ff02e7969e44afbe64ede4b2441ac-1979746227.ap-northeast-2.elb.amazonaws.com:8080/orders POST {"customerId":2, "productId":1}'
```
- 오토스케일이 어떻게 되고 있는지 확인한다.
```
➜  ~ kubectl get deploy -n coffee
NAME       READY   UP-TO-DATE   AVAILABLE   AGE
customer   1/1     1            1           8h
delivery   1/1     1            1           8h
gateway    2/2     2            2           6h24m
order      1/1     1            1           8h
product    1/1     1            1           8h
report     1/1     1            1           4h51m
```
- 어느정도 시간이 흐르면 스케일 아웃이 동작하는 것을 확인
```
➜  ~ kubectl get deploy -n coffee
NAME              READY   UP-TO-DATE   AVAILABLE   AGE
customer          1/1     1            1           23h
delivery          1/1     1            1           23h
gateway           2/2     2            2           21h
order             5/5     5            5           23h
product           5/5     5            5           23h
report            1/1     1            1           19h
```

- Availability 가 높아진 것을 확인 (siege)
```
Transactions:		         995 hits
Availability:		       82.64 %
Elapsed time:		       59.85 secs
Data transferred:	        0.29 MB
Response time:		        5.11 secs
Transaction rate:	       16.62 trans/sec
Throughput:		        0.00 MB/sec
Concurrency:		       84.94
Successful transactions:         995
Failed transactions:	         209
Longest transaction:	       15.26
Shortest transaction:	        0.02
```


## ConfigMap 설정
특정값을 k8s 설정으로 올리고 서비스를 기동 후, kafka 정상 접근 여부 확인한다.
```
    ➜  ~ kubectl describe cm report-config -n coffee
    Name:         report-config
    Namespace:    coffee
    Labels:       <none>
    Annotations:  <none>
    
    Data
    ====
    TEXT1:
    ----
    my-kafka.kafka.svc.cluster.local:9092
    TEXT2:
    ----
    9092
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
              brokers: ${TEXT1}
```
EKS 설치된 kafka에 정상 접근된 것을 확인할 수 있다. (해당 configMap TEXT1 값을 잘못된 값으로 넣으면 kafka WARN)
```
    2021-05-20 13:42:11.773 INFO 1 --- [pool-1-thread-1] o.a.kafka.common.utils.AppInfoParser : Kafka commitId : fa14705e51bd2ce5
    2021-05-20 13:42:11.785 INFO 1 --- [pool-1-thread-1] org.apache.kafka.clients.Metadata : Cluster ID: kJGw05_iTNOfms7RJu0JSw
    2021-05-20 13:42:14.049 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator : [Consumer clientId=consumer-3, groupId=report] Attempt to heartbeat failed since group is rebalancing
    2021-05-20 13:42:14.049 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator : [Consumer clientId=consumer-3, groupId=report] Revoking previously assigned partitions []
    2021-05-20 13:42:14.049 INFO 1 --- [container-0-C-1] o.s.c.s.b.k.KafkaMessageChannelBinder$1 : partitions revoked: []
    2021-05-20 13:42:14.049 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator : [Consumer clientId=consumer-3, groupId=report] (Re-)joining group
    2021-05-20 13:42:14.056 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.AbstractCoordinator : [Consumer clientId=consumer-3, groupId=report] Successfully joined group with generation 3
    2021-05-20 13:42:14.057 INFO 1 --- [container-0-C-1] o.a.k.c.c.internals.ConsumerCoordinator : [Consumer clientId=consumer-3, groupId=report] Setting newly assigned partitions [coffee-0]
    2021-05-20 13:42:14.064 INFO 1 --- [container-0-C-1] o.s.c.s.b.k.KafkaMessageChannelBinder$1 : partitions assigned: [coffee-0]
```

## Zero-downtime deploy
k8s의 무중단 서비스 배포 기능을 점검한다.
```
    ➜  ~ kubectl describe deploy order -n coffee
    Name:                   order
    Namespace:              coffee
    CreationTimestamp:      Thu, 20 May 2021 12:59:14 +0900
    Labels:                 app=order
    Annotations:            deployment.kubernetes.io/revision: 8
    Selector:               app=order
    Replicas:               4 desired | 4 updated | 4 total | 4 available | 0 unavailable
    StrategyType:           RollingUpdate
    MinReadySeconds:        0
    RollingUpdateStrategy:  50% max unavailable, 50% max surge
    Pod Template:
        Labels:       app=order
        Annotations:  kubectl.kubernetes.io/restartedAt: 2021-05-20T12:06:29Z
        Containers:
            order:
                Image:        740569282574.dkr.ecr.ap-northeast-2.amazonaws.com/order:v1
                Port:         8080/TCP
                Host Port:    0/TCP
                Liveness:     http-get http://:8080/actuator/health delay=120s timeout=2s period=5s #success=1 #failure=5
                Readiness:    http-get http://:8080/actuator/health delay=10s timeout=2s period=5s #success=1 #failure=10
```
기능 점검을 위해 order Deployment의 replicas를 4로 수정했다. 
그리고 위 Readiness와 RollingUpdateStrategy 설정이 정상 적용되는지 확인한다.
```
    ➜  ~ kubectl rollout status deploy/order -n coffee

    ➜  ~ kubectl get po -n coffee
    NAME                        READY   STATUS    RESTARTS   AGE
    customer-785f544f95-mh456   1/1     Running   0          5h40m
    delivery-557f4d7f49-z47bx   1/1     Running   0          5h40m
    gateway-6886bbf85b-58ms8    1/1     Running   0          4h56m
    gateway-6886bbf85b-mg9fz    1/1     Running   0          4h56m
    order-7978b484d8-6qsjq      1/1     Running   0          62s
    order-7978b484d8-h4hjs      1/1     Running   0          62s
    order-7978b484d8-rw2zk      1/1     Running   0          62s
    order-7978b484d8-x622v      1/1     Running   0          62s
    product-7f67966577-n7kqk    1/1     Running   0          5h40m
    report-5c6fd7b477-w9htj     1/1     Running   0          4h27m
    
    ➜  ~ kubectl get deploy -n coffee
    NAME       READY   UP-TO-DATE   AVAILABLE   AGE
    customer   1/1     1            1           8h
    delivery   1/1     1            1           8h
    gateway    2/2     2            2           6h1m
    order      2/4     4            2           8h
    product    1/1     1            1           8h
    report     1/1     1            1           4h28m
    
    ➜  ~ kubectl get po -n coffee
    NAME                        READY   STATUS    RESTARTS   AGE
    customer-785f544f95-mh456   1/1     Running   0          5h41m
    delivery-557f4d7f49-z47bx   1/1     Running   0          5h41m
    gateway-6886bbf85b-58ms8    1/1     Running   0          4h57m
    gateway-6886bbf85b-mg9fz    1/1     Running   0          4h57m
    order-7978b484d8-6qsjq      1/1     Running   0          115s
    order-7978b484d8-rw2zk      1/1     Running   0          115s
    order-84c9d7c848-mmw4b      0/1     Running   0          18s
    order-84c9d7c848-r64lc      0/1     Running   0          18s
    order-84c9d7c848-tbl8l      0/1     Running   0          18s
    order-84c9d7c848-tslfc      0/1     Running   0          18s
    product-7f67966577-n7kqk    1/1     Running   0          5h41m
    report-5c6fd7b477-w9htj     1/1     Running   0          4h28m
    
    ➜  ~ kubectl get po -n coffee
    NAME                        READY   STATUS    RESTARTS   AGE
    customer-785f544f95-mh456   1/1     Running   0          5h42m
    delivery-557f4d7f49-z47bx   1/1     Running   0          5h42m
    gateway-6886bbf85b-58ms8    1/1     Running   0          4h58m
    gateway-6886bbf85b-mg9fz    1/1     Running   0          4h58m
    order-84c9d7c848-mmw4b      1/1     Running   0          65s
    order-84c9d7c848-r64lc      1/1     Running   0          65s
    order-84c9d7c848-tbl8l      1/1     Running   0          65s
    order-84c9d7c848-tslfc      1/1     Running   0          65s
    product-7f67966577-n7kqk    1/1     Running   0          5h42m
    report-5c6fd7b477-w9htj     1/1     Running   0          4h29m
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
