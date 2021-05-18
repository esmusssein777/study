搭建 minikube

```
brew install minikube

minikube config set vm-driver hyperkit
minikube config set memory 8192
minikube config set cpus 4
minikube config set disk-size 40GB
minikube start
minikube dashboard
minikube service docker-awesome-server
minikube service docker-awesome-mysql
minikube status
minikube stop
minikube status
minikube start
```

开起 efk

```
minikube addons list
minikube addons enable efk
minikube addons open efk
```

开起 prometheus 和 grafana 监控

```
kubectl apply -f namespace.yaml

helm repo add prometheus-community https://prometheus-community.github.io/helm-charts

helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring

因为是 ClusterIP, 打开端口号
kubectl port-forward prometheus-grafana-5dbff499ff-vd7qp 3000:3000 -n monitoring

kubectl port-forward prometheus-prometheus-kube-prometheus-prometheus-0 9090:9090 -n monitoring

访问 localhost:3000 登陆
user:admin 
passwordprom-operator	
```

