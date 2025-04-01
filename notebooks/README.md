# LLM-as-a-Judge

This notebook showcases the different scenarios of LLM-as-a-Judge with concrete examples.

Its main purpose is to create alignment and structure the work that needs to be done to integrate AI-assisted judgments into the Search Relevance Workbench.

The different use cases where judgments are generated with language models:

1. The classic approach: Users define a judgment generation process by defining a query set, a LLM and a prompt. For every query in the query set the top n documents are retrieved by a search configuration and for each query-doc pair the LLM generates a judgment together with a reasoning statement.
2. The classic approach embedded in an experiment: Users run an experiment and want to create judgments "on-the-fly" by referencing an empty judgment list. The process of judgment.generation is identical to the first option. The difference is the user journey: create an empty list, run an experiment including judgment generation as part of the overarching process.
3. Filling in gaps: Users run an experiment and want to fill in any gaps. It is common to not have all query-doc pairs judged. With LLM-as-a-judge these gaps can be filled on the fly
4. Similarity-based judgments: Users want to generate judgments with LLMs that are based on the similarity of a provided reference statement to a retrieved document.

### Install Requirements and Start Jupyter

Create a virtual environment:

```
python3 -m venv .venv
```

Activate the virtual environment:
```
source .venv/bin/activate
```

Install the requirements:
```
pip3 install -r requirements.txt
```

Start Jupyter:
```
jupyter notebook
```

Open http://localhost:8888 in your browser (you might need to go for http://127.0.0.1:8888)

Install Ollama: https://ollama.com/download

The models used in the notebook are referenced.