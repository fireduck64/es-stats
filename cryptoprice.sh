
cd /home/clash/projects/es-stats.git

curl https://1209k.com/bp/price-json.php |./addtime.py | ./sendjson.sh cryptoprice
