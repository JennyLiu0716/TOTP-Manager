import base64
import secrets
from datetime import datetime
import redis
from flask import Flask, request, jsonify

app = Flask(__name__)

# Initialize Redis
redis_client = redis.StrictRedis(host='localhost', port=6379, decode_responses=True)



@app.route('/create_user', methods=['POST'])
def create_user():
    username = secrets.token_hex(128)
    secret_key = secrets.token_hex(16)

    while redis_client.exists(f"user:{username}"):
        username = secrets.token_hex(128)

    redis_client.set(f"user:{username}", "")

    user_credentials = f"{username}:{secret_key}"
    encoded_credentials = base64.b64encode(user_credentials.encode()).decode()

    return jsonify({'credentials': encoded_credentials}), 200

@app.route('/upload', methods=['POST'])
def upload():
    data = request.json
    username = data.get('username')
    encrypted_data = data.get('encrypted_data')

    if not username or not encrypted_data:
        return jsonify({'error': 'Username and encrypted data are required'}), 400

    if not redis_client.exists(f"user:{username}"):
        return jsonify({'error': 'User not found'}), 404
    user_key = f"user_data:{username}"

    update_time = datetime.now().timestamp()
    redis_client.hset(f"user_data:{username}", "username", username)
    redis_client.hset(f"user_data:{username}", "encrypted_data", encrypted_data)
    redis_client.hset(f"user_data:{username}", "update_time", update_time)
    redis_client.expire(user_key, 2592000)
    return jsonify({'message': 'Data uploaded successfully'}), 200

@app.route('/sync', methods=['POST'])
def sync():
    data = request.json
    username = data.get('username')

    if not username:
        return jsonify({'error': 'Username is required'}), 400

    if not redis_client.exists(f"user_data:{username}"):
        return jsonify({'error': 'User not found'}), 404

    encrypted_data = redis_client.hget(f"user_data:{username}", "encrypted_data")
    return jsonify({'encrypted_data': encrypted_data}), 200

@app.route('/ping', methods=['POST', 'GET'])
def ping():
    return jsonify({"res": "pang"}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=443, ssl_context=('server.crt', 'server.key'), debug=True)
