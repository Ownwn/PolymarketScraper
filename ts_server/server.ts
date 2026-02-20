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

const wallet = new Wallet(privateKey)

const client = new ClobClient(
  "https://clob.polymarket.com",
  137, wallet,
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

app.get("/trades", async (_req: any, res: any) => {
  try {
      const orders = await client.getTrades({ // todo not hardcode
          market: "0x4b02efe53e631ada84681303fd66d79ad615f3d2b6a28b4633d43d935f89af58",
      },
          true);
      console.log("got orders: ", JSON.stringify(orders, null, 2));
    res.json(orders);
  } catch (error: any) {
    console.error("Error fetching open orders:", error);
    res.status(500).json({ error: error.message });
  }
});

app.listen(PORT, () => {
  console.log(`Polymarket Bridge Server (Deno) listening on port ${PORT}`);
});