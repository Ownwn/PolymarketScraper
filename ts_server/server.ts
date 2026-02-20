import express from "npm:express@4.18.2";
import { ClobClient } from "npm:@polymarket/clob-client@^5.2.3";
import {Wallet} from "ethers";


const app = express();
app.use(express.json());

const PORT = 3000;

const credentials = {
  key: process.env.POLY_API_KEY,
  secret: process.env.POLY_API_SECRET,
  passphrase: process.env.POLY_API_PASSPHRASE
};

if (!credentials.key || !credentials.secret || !credentials.passphrase) {
    throw Error("One of credentials is not set")
}

const privateKey = process.env.PRIVATE_KEY
if (!privateKey) {
    throw Error("PRIVATE_KEY environment variable is not set")
}

const proxyAddress = process.env.POLY_PROXY_ADDRESS
if (!proxyAddress) {
    throw Error("missing proxy address (public)")
}

const client = new ClobClient(
  "https://clob.polymarket.com",
  137, new Wallet(privateKey),
  credentials,
  1,
  proxyAddress
);

// Get order book
app.get("/book", async (req: any, res: any) => {
  try {
    const tokenId = req.query.token_id as string;
    if (!tokenId) {
      return res.status(400).json({ error: "token_id query parameter is required" });
    }
    const book = await client.getMarketOrderBook(tokenId);
    res.json(book);
  } catch (error: any) {
    console.error("Error fetching order book:", error);
    res.status(500).json({ error: error.message });
  }
});

// Get open orders
app.get("/orders", async (_req: any, res: any) => {
  try {
      console.log("fetching orders")
    const orders = await client.getOpenOrders();
      console.log("got orders: ", orders)
    res.json(orders);
  } catch (error: any) {
    console.error("Error fetching open orders:", error);
    res.status(500).json({ error: error.message });
  }
});

// Place order
app.post("/order", async (req: any, res: any) => {
  try {
    const { tokenID, side, price, size } = req.body;
    
    if (!tokenID || !side || !price || !size) {
      return res.status(400).json({ error: "Missing required fields: tokenID, side, price, size" });
    }

    const order = await client.createAndPostOrder(
      { 
        tokenID, 
        price, 
        size, 
        side: side.toUpperCase() as any 
      },
      { tickSize: "0.01", negRisk: false }
    );
    
    res.json(order);
  } catch (error: any) {
    console.error("Error placing order:", error);
    res.status(500).json({ error: error.message });
  }
});

// Cancel all orders
app.delete("/orders", async (_req: any, res: any) => {
  try {
    const result = await client.cancelAllOrders();
    res.json(result);
  } catch (error: any) {
    console.error("Error cancelling all orders:", error);
    res.status(500).json({ error: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`Polymarket Bridge Server (Deno) listening on port ${PORT}`);
});