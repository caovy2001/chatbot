from langchain.vectorstores import FAISS
from langchain.document_loaders import TextLoader
from langchain.embeddings import HuggingFaceEmbeddings
from langchain.text_splitter import CharacterTextSplitter
from langchain.embeddings.openai import OpenAIEmbeddings
import pickle
from dotenv import load_dotenv
load_dotenv()
# embeddings = OpenAIEmbeddings()
embeddings = HuggingFaceEmbeddings(model_name="keepitreal/vietnamese-sbert")

def train(payload): 
    userId = payload["user_id"]
    loader = TextLoader(file_path="training_data_" + userId + ".txt")
    documents = loader.load()
    text_splitter = CharacterTextSplitter(separator="\n", chunk_size=30, chunk_overlap=1)
    documents = text_splitter.split_documents(documents)
    db = FAISS.from_documents(documents, embeddings)
    with open("model/" + userId + ".model", "wb") as f:
        pickle.dump((db), f)
    print("Train for user " + userId + " successfully!")
        
def predict(payload):
    message = payload["message"]
    userId = payload["userId"]
    intentNames = payload["intentNames"]
    
    with open("model/" + userId + ".model", "rb") as f:
        db = pickle.load(f)
    similar_response = db.similarity_search(message, k=10)
    patternWithId = []
    i = 0
    for response in similar_response:
        patternWithId.append({
            'id': i,
            'content': response.page_content
        })
        print(patternWithId[i])
        i += 1
    
    percentagePerWord = 1 / len(message.split(' '))
    # patternPercentage = {
    #     {id_pattern}: {percentage} 
    # }
    patternPercentage = {}
    for word in message.split(' '):
        for content in patternWithId:
            patternContent = content['content'].split("|")[0].strip() 
            if (word.lower() in patternContent.lower()):
                if (str(content['id']) in patternPercentage):
                    patternPercentage[str(content['id'])] = patternPercentage[str(content['id'])] + percentagePerWord + (1 / len(patternContent.split(' ')))                    
                else:
                    patternPercentage[str(content['id'])] = percentagePerWord + (1 / len(patternContent.split(' ')))
    print(patternPercentage)
    
    resPatternIds = []
    for patternId in patternPercentage.keys():
        patternContent = patternWithId[int(patternId)]['content']
        intentName = patternContent.split('|')[1].strip()
        if (patternPercentage[patternId] >= 1 and intentName in intentNames): 
            resPatternIds.append(patternId)
    
    # {"Nói tên": 2, "Nói tuổi": 3}
    resIntentsWithCount = {}
    for resPatternId in resPatternIds: 
        patternContent = patternWithId[int(resPatternId)]['content']
        intentName = patternContent.split('|')[1].strip()
        if (intentName in resIntentsWithCount):
            resIntentsWithCount[intentName] = resIntentsWithCount[intentName] + 1
        else:
            resIntentsWithCount[intentName] = 1
    print(resIntentsWithCount)
    
    resIntentName = None
    highestCount = 0
    for intentName in resIntentsWithCount.keys():
        if (resIntentsWithCount[intentName] > highestCount):
            resIntentName = intentName
            highestCount = resIntentsWithCount[intentName]
    
    return {
        "intentName": resIntentName
    }
            
    