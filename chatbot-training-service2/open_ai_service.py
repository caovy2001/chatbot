from langchain.prompts import PromptTemplate
from langchain.chat_models import ChatOpenAI
from langchain.chains import LLMChain
import time
from dotenv import load_dotenv
import os

# Set an environment variable
os.environ['OPENAI_API_KEY'] = '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'

# load_dotenv()
# llm = ChatOpenAI(temperature=1, model="gpt-3.5-turbo-16k-0613")
# template="{message}"

# prompt = PromptTemplate(
#     input_variables=['message'],
#     template=template
# )
# chain = LLMChain(llm=llm, prompt=prompt)

# def generate_response(message):
#     response = chain.run(message=message)
#     return response


# if __name__ == '__main__':
#     current_time_long1 = int(time.time())
#     result = generate_response('tôi năm nay 20 tuổi')
#     current_time_long2 = int(time.time())
#     print("Time: " + str(current_time_long2 - current_time_long1))
#     print(result)
    
def askGpt(payload):
    message = payload['message']
    llm = ChatOpenAI(temperature=1, model="gpt-3.5-turbo-16k-0613")
    template="{message}"
    prompt = PromptTemplate(
    input_variables=['message'],
        template=template
    )
    chain = LLMChain(llm=llm, prompt=prompt)
    response = chain.run(message=message)
    print('Response from GPT: ' + response)
    return {
        'result': response
    }
    
    