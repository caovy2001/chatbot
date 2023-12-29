from flask import Flask, jsonify, request
import json
from flask_cors import CORS
import embedding_service

app = Flask(__name__)
CORS(app)

@app.route("/predict", methods=['POST'])
def predictAPI():
    payload = json.loads(request.data)
    
    return jsonify(embedding_service.predict(payload))

@app.route("/train", methods=['POST'])
def trainAPI():
    payload = json.loads(request.data)
    
    return jsonify(embedding_service.train(payload))

if __name__ == "__main__":
    app.run(threaded=True, host="0.0.0.0", port=5001)
