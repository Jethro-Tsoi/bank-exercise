Account entries are uploaded each day to our server in a transaction csv file. Example in resources.
We must process these transactions and update the balance and entries of each account.
Bank interface is exposed to clients, its imperative that balance and entries are consistent.
Our bank clients expect transactions to be processed rapidly in a performant manner.
Multiple transaction files may be uploaded every day.

A nice-to-have requirement, is that all transactions are reported to our legacy server.  
Legacy server is slow, but can handle multiple concurrent requests.

Account entries/transactions can definitely be processed and made available to clients, before reporting to legacy server.

Other developers will have to debug any issues, so please make it easy to read and comprehend.
We value simple and easy to understand code. 

Please make the code as close to production quality as possible given time considerations.