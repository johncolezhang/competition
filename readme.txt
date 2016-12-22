1.网点id及经纬度，供124个网点
Site_id（e.g. A001） 
Lng 
Lat

2.配送点id及经纬度，共9214个配送点
Spot_id（e.g. B0001） 
Lng 
Lat

3.商户id及经纬度，共598个商户
Shop_id（e.g. S001） 
Lng 
Lat

4.电商订单，共9214笔电商订单，总包裹量为229780
Order_id（e.g. F0001） 
Spot_id（e.g. B0001） 
Site_id（e.g. A001）
Num
F : A ==>> B

5.同城O2O订单,共3273笔O2O订单，总包裹量为8856
Order_id（e.g. E0001）
Spot_id（e.g. B0001）
Shop_id（e.g. S001）
Pickup_time  
Delivery_time
Num
E : S ==>> B

6.快递员id列表，最多1000位小件员
Courier_id（e.g. D0001）

调度计划：（ResultOrder）
Courier_id（e.g. D0001）
Addr 网点或配送点或商户的id
Arrival_time到达时间
Departure离开时间
Amount取为+，送为-
Order_id