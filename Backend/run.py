"""This script initializes and runs the Flask application."""
from app import create_app

app = create_app()

if __name__ == "__main__":
    app.run(debug=True,host='0.0.0.0', port=5000, ssl_context=('cert.pem', 'key.pem'))
