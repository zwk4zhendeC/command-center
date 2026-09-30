
### 安装NGINX Ingress Controller

#### 1.安装cni

1. 确定CNI是否安装，下面是已经安装的情况:
```bash
kubectl get nodes
kubectl get pods -A
```

理想状态：
```bash
# 节点是Ready状态
NAME            STATUS   ROLES           AGE   VERSION
vm-0-7-ubuntu   Ready    control-plane   ...   v1.37.0
# CoreDNS是run状态
kube-system     calico-kube-controllers-78457f854f-lsshk    1/1     Running   0          4d6h
kube-system     calico-node-tvmdw                           1/1     Running   0          4d6h
kube-system     coredns-559f6c778d-5p59r                    1/1     Running   0          11d
kube-system     coredns-559f6c778d-vnh69                    1/1     Running   0          11d
```

2. 安装
```bash
kubectl apply -f https://raw.githubusercontent.com/projectcalico/calico/v3.31.0/manifests/calico.yaml
```

#### 2.安装Ingress-NGINX
1. 安装
```bash
helm repo add ingress-nginx https://kubernetes.github.io/ingress-nginx
helm repo update
helm upgrade ingress-nginx ingress-nginx/ingress-nginx \
  --namespace ingress-nginx \
  --set controller.hostNetwork=true \
  --set controller.dnsPolicy=ClusterFirstWithHostNet
```
- controller.hostNetwork=true： Ingress Controller Pod 使用 Node 的网络 namespace。
- controller.dnsPolicy=ClusterFirstWithHostNet：虽然使用 hostNetwork，但 Pod 的 DNS 仍然优先使用 Kubernetes DNS。

2. 查看pod和svc创建情况
```bash
kubectl get pods -n ingress-nginx
kubectl get svc -n ingress-nginx

# 应该能看到
NAME                                        READY   STATUS
ingress-nginx-controller-xxxxxxxx-xxxxx   1/1     Running
NAME                       TYPE       CLUSTER-IP      EXTERNAL-IP   PORT(S)
ingress-nginx-controller   NodePort   10.x.x.x        <none>        80:3xxxx/TCP,443:3xxxx/TCP
```

## 部署监控组件

监控主机需要部署的第三方组件：
1. 


### 主机指标监控