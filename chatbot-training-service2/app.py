from flask import Flask, jsonify, request
import json
from flask_cors import CORS
import embedding_service
import open_ai_service

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

@app.route("/ask-gpt", methods=['POST'])
def askGptAPI():
    payload = json.loads(request.data)
    
    return jsonify(open_ai_service.askGpt(payload))

if __name__ == "__main__":
    app.run(threaded=True, host="0.0.0.0", port=5001)
