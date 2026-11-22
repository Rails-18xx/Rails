import pandas as pd
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import MinMaxScaler
import numpy as np
import onnx
import onnxruntime

# --- 1. CONFIGURATION ---
CSV_FILE = "../training_data.csv"
BATCH_SIZE = 64
EPOCHS = 50
LEARNING_RATE = 0.001
MODEL_PATH = "rails_1835_v1.onnx"

# --- 2. DATA LOADING ---
print(f"Loading {CSV_FILE}...")
# Load data, assuming no header in CSV (based on your output)
# If your CSV has no header, pandas assigns 0, 1, 2...
df = pd.read_csv(CSV_FILE, header=None)

# Split Features (X) and Target (y)
# The last column is the 'Reward' (Cash), everything else is State
X_raw = df.iloc[:, :-1].values.astype(np.float32)
y_raw = df.iloc[:, -1].values.astype(np.float32).reshape(-1, 1)

INPUT_SIZE = X_raw.shape[1]
print(f"Detected Input Features: {INPUT_SIZE}")
print(f"Total Samples: {len(df)}")

# Normalize Inputs (Crucial for Neural Networks)
# We save the scaler to apply the same math in Java later
scaler = MinMaxScaler()
X_scaled = scaler.fit_transform(X_raw)

# Normalize Target (Optional, but helps convergence)
# We scale cash to roughly 0-1 range (assuming max cash ~10,000)
y_scaled = y_raw / 10000.0

# Train/Test Split
X_train, X_test, y_train, y_test = train_test_split(X_scaled, y_scaled, test_size=0.2)

# --- 3. PYTORCH DATASET ---
class RailsDataset(Dataset):
    def __init__(self, X, y):
        self.X = torch.tensor(X)
        self.y = torch.tensor(y)
    def __len__(self): return len(self.X)
    def __getitem__(self, idx): return self.X[idx], self.y[idx]

train_loader = DataLoader(RailsDataset(X_train, y_train), batch_size=BATCH_SIZE, shuffle=True)
test_loader = DataLoader(RailsDataset(X_test, y_test), batch_size=BATCH_SIZE)

# --- 4. THE NETWORK ARCHITECTURE ---
class ValueNetwork(nn.Module):
    def __init__(self, input_size):
        super(ValueNetwork, self).__init__()
        # A "Wide" network to capture feature interactions
        self.model = nn.Sequential(
            nn.Linear(input_size, 512),
            nn.ReLU(),
            nn.Linear(512, 256),
            nn.ReLU(),
            nn.Linear(256, 128),
            nn.ReLU(),
            nn.Linear(128, 1)  # Output: Single Value (Cash)
        )

    def forward(self, x):
        return self.model(x)

# Initialize
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
model = ValueNetwork(INPUT_SIZE).to(device)
criterion = nn.MSELoss()
optimizer = optim.Adam(model.parameters(), lr=LEARNING_RATE)

# --- 5. TRAINING LOOP ---
print("Starting Training...")
model.train()
for epoch in range(EPOCHS):
    total_loss = 0
    for batch_X, batch_y in train_loader:
        batch_X, batch_y = batch_X.to(device), batch_y.to(device)
        
        optimizer.zero_grad()
        predictions = model(batch_X)
        loss = criterion(predictions, batch_y)
        loss.backward()
        optimizer.step()
        
        total_loss += loss.item()
    
    if (epoch+1) % 5 == 0:
        print(f"Epoch {epoch+1}/{EPOCHS} | Loss: {total_loss / len(train_loader):.6f}")

# --- 6. EVALUATION ---
model.eval()
with torch.no_grad():
    test_loss = 0
    for batch_X, batch_y in test_loader:
        batch_X, batch_y = batch_X.to(device), batch_y.to(device)
        preds = model(batch_X)
        test_loss += criterion(preds, batch_y).item()
    print(f"Final Test MSE: {test_loss / len(test_loader):.6f}")

# --- 7. EXPORT TO ONNX (For Java) ---
print(f"Exporting to {MODEL_PATH}...")
dummy_input = torch.randn(1, INPUT_SIZE, device=device)
torch.onnx.export(
    model, 
    dummy_input, 
    MODEL_PATH,
    input_names=['input'], 
    output_names=['output'],
    dynamic_axes={'input': {0: 'batch_size'}, 'output': {0: 'batch_size'}}
)
print("Done. Model ready for Java.")