import dataservice.Impl.OperationDataServiceImpl;
import dataservice.OperationDataService;
import enums.SubscriptionState;
import enums.WayOfCall;
import enums.WayOfUnsubscribe;
import timetask.TimerManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Scanner;

/**
 * Created by zlk on 2018/10/20
 */
public class Main {
    //mysql驱动包名
    private static final String DRIVER_NAME = "com.mysql.jdbc.Driver";
    //数据库连接地址
    private static final String URL = "jdbc:mysql://localhost:3306/mobileBusiness";
    //用户名
    private static final String USER_NAME = "root";
    //密码
    private static final String PASSWORD = "root";
    static Connection connection = null;

    public static void main(String[] args){

        //连接数据库
        try {
            //加载mysql的驱动类
            Class.forName(DRIVER_NAME);
            //获取数据库连接
            connection = DriverManager.getConnection(URL, USER_NAME, PASSWORD);
            System.out.println("Database connected!");
        } catch (Exception e) {
            throw new IllegalStateException("Cannot connect the database");
        }

        OperationDataService operationDataService = new OperationDataServiceImpl(connection);
        new TimerManager(connection);
//        operationDataService.ConnectDatabase();

        long startTime = 0;
        long endTime = 0;

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("请选择您想进行的操作： (0)查询套餐 (1)订购套餐 (2)退订套餐 (3)通话情况下的资费生成 (4)使用流量情况下的资费生成 (5)用户月账单的生成");
            int operation = sc.nextInt();
            sc.nextLine();
            System.out.println("请输入您的手机号：");
            String phoneNumber = sc.nextLine();
            switch (operation){
                case 0:
                    System.out.println("您是想查询当前套餐还是历史套餐？ (0)当前套餐 (1)历史套餐");
                    int choose = sc.nextInt();
                    sc.nextLine();
                    startTime = System.nanoTime();
                    if(choose == 0){
                        operationDataService.getSubscriptionInProcess(phoneNumber,SubscriptionState.ON);
                    }else{
                        operationDataService.getSubscriptionInProcess(phoneNumber,SubscriptionState.END);
                    }
                    endTime = System.nanoTime();
                    System.out.println("查询套餐所用时间为"+(endTime-startTime)/1000+"微秒");
                    break;
                case 1:
                    operationDataService.showAllPackage();
                    System.out.println("请输入您想订购的套餐Id:");
                    String p_id = sc.nextLine();
                    startTime = System.nanoTime();
                    operationDataService.order(phoneNumber,p_id);
                    endTime = System.nanoTime();
                    System.out.println("订购套餐所用时间为"+(endTime-startTime)/1000+"微秒");
                    break;
                case 2:
                    System.out.println("请输入您想退订的套餐：");
                    String p_id1 = sc.nextLine();
                    System.out.println("您是想退订操作立即生效还是次月生效？ (0)立即生效 (1)次月生效");
                    int choose1 = sc.nextInt();
                    sc.nextLine();
                    startTime = System.nanoTime();
                    if(choose1 == 0){
                        operationDataService.unsubscribe(phoneNumber,p_id1,WayOfUnsubscribe.EFFECTIMMEDIATELY);
                    }else{
                        operationDataService.unsubscribe(phoneNumber,p_id1,WayOfUnsubscribe.EFFECTNEXTMONTH);
                    }
                    endTime = System.nanoTime();
                    System.out.println("退订套餐所用时间为"+(endTime-startTime)/1000+"微秒");
                    break;
                case 3:
                    System.out.println("您的通话方式为： (0)拨打 (1)接听");
                    int choose2 = sc.nextInt();
                    sc.nextLine();
                    System.out.println("您的通话时间为：");
                    int callTime = sc.nextInt();
                    sc.nextLine();
                    System.out.println("开始通话...");
                    startTime = System.nanoTime();
                    if(choose2==0){
                        operationDataService.call(phoneNumber,callTime,WayOfCall.DIAL);
                    }else{
                        operationDataService.call(phoneNumber,callTime,WayOfCall.ANSWER);
                    }
                    endTime = System.nanoTime();
                    System.out.println("通话情况下生成资费所用时间为"+(endTime-startTime)/1000+"微秒");
                    break;
                case 4:
                    System.out.println("您在哪里使用流量？ (0)本地 (1)外地");
                    int choose3 = sc.nextInt();
                    sc.nextLine();
                    System.out.println("您使用流量多少G?");
                    float traffic = sc.nextFloat();
                    sc.nextLine();
                    startTime = System.nanoTime();
                    if(choose3==0){
                        operationDataService.useTrafficInTheLocalArea(phoneNumber,traffic);
                    }else {
                        operationDataService.useTrafficInTheField(phoneNumber,traffic);
                    }
                    endTime = System.nanoTime();
                    System.out.println("使用流量情况下生成资费所用时间为"+(endTime-startTime)/1000+"微秒");
                    break;
                case 5:
                    startTime = System.nanoTime();
                    operationDataService.createBill(phoneNumber);
                    endTime = System.nanoTime();
                    System.out.println("生成月账单所用时间为"+(endTime-startTime)/1000+"微秒");
                    break;
                default:
                    break;
            }
        }
    }
}