# 📡 Tài liệu Giao thức Client-Server

## 📋 Tổng quan

Server sử dụng giao thức TCP Socket với cơ chế message-based để giao tiếp với client. Mỗi kết nối được quản lý bởi một `Session` với 2 threads riêng biệt cho việc gửi và nhận dữ liệu.

## 🔌 Kiến trúc kết nối

### 1. **Khởi tạo kết nối**
```
Client → [TCP Connect] → Server:14445
         ↓
    ServerSocket.accept()
         ↓
    Create new Session
         ↓
    Start MessageCollector thread (Receive)
         ↓
    Send Session Key (cmd: -27)
         ↓
    Start MessageSender thread (Send)
```

### 2. **Cấu trúc Session**
```java
Session {
    - id: Session ID tự động tăng
    - socket: TCP Socket connection
    - sendThread: Thread gửi messages
    - receiveThread: Thread nhận messages
    - player: Player object khi đã login
    - ipAddress: Client IP
    - curR, curW: XOR key cursors
}
```

## 📦 Message Protocol

### 1. **Cấu trúc Message**

#### **Unencrypted Message (trước khi connected)**
```
[Command: 1 byte] [Size: 2 bytes] [Data: n bytes]
```

#### **Encrypted Message (sau khi connected)**
```
[Command XOR: 1 byte] [Size XOR: 2 bytes] [Data XOR: n bytes]
```

### 2. **Đặc biệt: Big Messages**
Một số command sử dụng 3 bytes cho size:
- Commands: -32, -66, -74, 11, -67, -87, 66
```
[Command] [Size_byte1 - 128] [Size_byte2 - 128] [Size_byte3 - 128] [Data]
```

### 3. **XOR Encryption**
- Server sử dụng XOR với key array để mã hóa/giải mã
- Mỗi byte được XOR với `KEYS[cursor++]`
- Cursor reset về 0 khi đạt cuối array

```java
// Encrypt
encrypted = data ^ KEYS[curW++]

// Decrypt  
decrypted = data ^ KEYS[curR++]
```

## 📨 Luồng xử lý Message

### **Receive Flow (Client → Server)**
```
1. MessageCollector.readMessage()
   ├─ Read command byte
   ├─ Read size (2 bytes)
   ├─ Read data (n bytes)
   ├─ XOR decrypt if connected
   └─ Create Message object
   
2. Controller.onMessage(session, message)
   └─ Process by command ID
```

### **Send Flow (Server → Client)**
```
1. Session.sendMessage(message)
   └─ Add to sendingMessage queue

2. MessageSender thread loop
   ├─ Get message from queue
   ├─ XOR encrypt if connected
   ├─ Write command + size + data
   └─ Flush to socket
```

## 🎮 Command Protocol

### **Authentication Flow**
```
1. GET_SESSION_ID (-28) → Client requests session
2. SESSION_KEY (-27)   → Server sends encryption key
3. CLIENT_INFO (2)     → Client sends version info
4. LOGIN (-101)        → Login credentials
5. SELECT_PLAYER (1)   → Choose character
```

### **Các nhóm Command chính**

#### **System Commands**
| CMD | Value | Description | Direction |
|-----|-------|-------------|-----------|
| GET_SESSION_ID | -28 | Yêu cầu session | C→S |
| SESSION_KEY | -27 | Gửi key mã hóa | S→C |
| CLIENT_INFO | 2 | Thông tin client | C→S |
| CLIENT_OK | 13 | Xác nhận client ready | C→S |

#### **Authentication & Player**
| CMD | Value | Description | Direction |
|-----|-------|-------------|-----------|
| LOGIN | -101 | Đăng nhập | C→S |
| LOGOUT | 0 | Đăng xuất | C→S |
| SELECT_PLAYER | 1 | Chọn nhân vật | C→S |
| CREATE_PLAYER | 2 | Tạo nhân vật | C→S |
| DELETE_PLAYER | 3 | Xóa nhân vật | C→S |

#### **Game Actions**  
| CMD | Value | Description | Direction |
|-----|-------|-------------|-----------|
| UPDATE_MAP | 6 | Update map data | S→C |
| REQUEST_SKILL | 9 | Yêu cầu use skill | C→S |
| PLAYER_ATTACK_PLAYER | -60 | PvP attack | C→S |
| CHAT_PRIVATE | -72 | Chat riêng | C→S |

#### **Trading & Shop**
| CMD | Value | Description | Direction |
|-----|-------|-------------|-----------|
| BUY_ITEM | 6 | Mua item | C→S |
| SELL_ITEM | 7 | Bán item | C→S |
| TRANSACTION | -86 | Giao dịch | C→S |
| KIGUI | -100 | Ký gửi shop | C→S |

## 🔒 Security Features

### 1. **IP Limiting**
- Giới hạn số connection từ 1 IP (`MAX_PER_IP`)
- Track connections trong `CLIENTS` HashMap

### 2. **Timeout Protection**
- Session timeout: 180 seconds không có message
- Auto disconnect khi timeout

### 3. **Size Validation**
- Max message size: 1024 bytes
- Throw exception nếu vượt quá

## 💾 Data Serialization

### **Write Data (DataOutputStream)**
```java
// Write primitive types
writer.writeByte(value)
writer.writeShort(value)  
writer.writeInt(value)
writer.writeLong(value)
writer.writeBoolean(value)
writer.writeUTF(string)

// Write arrays
for(item : array) {
    writer.writeShort(item.id)
    writer.writeByte(item.quantity)
}
```

### **Read Data (DataInputStream)**
```java
// Read primitive types
byte val = reader.readByte()
short val = reader.readShort()
int val = reader.readInt()
long val = reader.readLong()
boolean val = reader.readBoolean()
String val = reader.readUTF()

// Read arrays
int size = reader.readByte()
for(int i = 0; i < size; i++) {
    items[i].id = reader.readShort()
    items[i].quantity = reader.readByte()
}
```

## 🌟 Ví dụ Message Flow

### **Login Flow Example**
```
// 1. Client connect
TCP Connect → 127.0.0.1:14445

// 2. Server send session key
S→C: Message(-27) {
    writeByte(KEYS.length)
    writeByte(KEYS[0])
    for(i=1..n) writeByte(KEYS[i] ^ KEYS[i-1])
    writeUTF("localhost")
    writeInt(14445)
    writeBoolean(false)
}

// 3. Client send login
C→S: Message(-101) {
    writeUTF(username)
    writeUTF(password)
    writeUTF(version)
}

// 4. Server response
S→C: Message(LOGIN_SUCCESS) {
    writeByte(status)
    writeInt(userId)
    // player data...
}
```

### **Buy Item Example**
```java
// Client request
Message msg = new Message(6); // BUY_ITEM
msg.writer().writeByte(typeBuy); // 0=gold, 1=gem
msg.writer().writeShort(itemId);
msg.writer().writeShort(quantity);

// Server process
Controller.onMessage() {
    case 6: // BUY_ITEM
        byte type = msg.reader().readByte();
        short id = msg.reader().readShort();
        short qty = msg.reader().readShort();
        ShopService.buyItem(player, type, id, qty);
}
```

## 🔄 Threading Model

```
Main Thread
    └─ ServerSocket.accept()
        └─ Create Session
            ├─ MessageCollector Thread
            │   └─ Read from socket → Controller.onMessage()
            └─ MessageSender Thread
                └─ Queue.poll() → Write to socket
                
Game Threads (parallel)
    ├─ Boss Update (100ms)
    ├─ Pho Ban Update (500ms)  
    ├─ Auto Save (5 min)
    └─ DHVT Update (100ms)
```

## 📊 Performance Considerations

1. **Thread per Connection**: Mỗi client = 2 threads (send/receive)
2. **Message Queue**: Async send với ArrayList queue
3. **TCP NoDelay**: Tắt Nagle algorithm cho low latency
4. **Small Buffer**: Max 1KB/message để tránh memory bloat

## 🚀 Khuyến nghị khi port sang Rust

1. **Async I/O**: Dùng Tokio thay vì thread-per-connection
2. **Binary Protocol**: Giữ nguyên hoặc dùng bincode/rkyv
3. **Encryption**: Có thể upgrade lên AES thay vì XOR
4. **WebSocket**: Hỗ trợ web client với tungstenite
5. **Message Queue**: Dùng channels (mpsc/broadcast)
6. **Zero-copy**: Dùng bytes crate cho buffer management
