FROM python:3.9-slim
WORKDIR /app
COPY . /app
RUN pip install pandas torch scikit-learn onnx onnxruntime
CMD ["python", "python/trainnetwork.py"]