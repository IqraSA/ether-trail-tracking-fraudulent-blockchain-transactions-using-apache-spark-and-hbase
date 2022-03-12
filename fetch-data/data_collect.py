import sys
import infura
from datetime import datetime

class DataCollect:
    def __init__(self):
        self.infura = infura.INFURA(
            _project_id="",
            _project_scrt=""
        )

    def get_block_by_number(self):
        # https://etherscan.io/block/8900401
        block_hash = "0xab55941eddeb3103febf60ec325dfd8d8ca57fac0104d4ae1d63749fb68db465"
        count = 0
        max_count = 100
        while (count != max_count and block_hash != None):
            response = self.infura.get_block_by_hash(block_hash)
            result = response["result"]
            timestamp = int(result["timestamp"], 16)
            file_name = "data/" + datetime.utcfromtimestamp(timestamp).strftime('%Y-%m-%d_%H-%M-%S') + "_" + str(int(result["number"], 16)) + ".txt"
            with open(file_name, 'w') as op_file:
                block_hash = result["parentHash"]
                transactions = result["transactions"]
                for i in range(len(transactions)):
                    self.parse_transaction(op_file, transactions[i])
            count += 1

    def parse_transaction(self, op_file, txn):
        to_addr = txn["to"]
        from_addr = txn["from"]
        value = str(int(txn["value"], 16))
        hash = txn["hash"]
        txn_idx = str(int(txn["blockNumber"], 16))+str(int(txn["transactionIndex"], 16))
        if (to_addr and from_addr and value and txn_idx and hash):
            op = f'{hash},{to_addr},{from_addr},{value},{txn_idx}' + "\n"
            op_file.write(op)



if __name__ == "__main__":
    data_collector = DataCollect()
    block = data_collector.get_block_by_number()
