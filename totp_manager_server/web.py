import pyotp
import qrcode
import io
from flask import Flask, request, jsonify, send_file, render_template_string
from werkzeug.security import generate_password_hash, check_password_hash

app = Flask(__name__)

users = {}

HTML_TEMPLATE = '''
<!doctype html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <title>Register and Login</title>
  </head>
  <body>
    <h2>Register</h2>
    <form action="/register" method="post" enctype="multipart/form-data">
      <label for="username">Username:</label>
      <input type="text" id="username" name="username"><br><br>
      <label for="password">Password:</label>
      <input type="password" id="password" name="password"><br><br>
      <input type="submit" value="Register">
    </form>
    <br>
    <h2>Login</h2>
    <form action="/login" method="post">
      <label for="username">Username:</label>
      <input type="text" id="username" name="username"><br><br>
      <label for="password">Password:</label>
      <input type="password" id="password" name="password"><br><br>
      <label for="totp">TOTP Code:</label>
      <input type="text" id="totp" name="totp"><br><br>
      <input type="submit" value="Login">
    </form>
  </body>
</html>
'''

@app.route('/')
def index():
    return render_template_string(HTML_TEMPLATE)

@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'POST':
        username = request.form.get('username')
        password = request.form.get('password')

        if not username or not password:
            return jsonify({'error': 'Username and password are required'}), 400

        if username in users:
            return jsonify({'error': 'Username already exists'}), 400

        password_hash = generate_password_hash(password)

        shared_key = pyotp.random_base32()

        users[username] = {
            'password_hash': password_hash,
            'shared_key': shared_key
        }

        totp_uri = pyotp.totp.TOTP(shared_key).provisioning_uri(name=username, issuer_name="MyApp")
        qr = qrcode.make(totp_uri)
        buffered = io.BytesIO()
        qr.save(buffered, format="PNG")
        buffered.seek(0)

        return send_file(buffered, mimetype="image/png")

    return render_template_string(HTML_TEMPLATE)

@app.route('/login', methods=['POST'])
def login():
    username = request.form.get('username')
    password = request.form.get('password')
    totp_code = request.form.get('totp')

    if not username or not password or not totp_code:
        return jsonify({'error': 'Username, password, and TOTP code are required'}), 400

    user = users.get(username)
    if not user or not check_password_hash(user['password_hash'], password):
        return jsonify({'error': 'Invalid username or password'}), 401

    totp = pyotp.TOTP(user['shared_key'])
    if not totp.verify(totp_code):
        return jsonify({'error': 'Invalid TOTP code'}), 401

    return jsonify({'message': 'Login successful'}), 200

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
