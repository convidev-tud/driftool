#  Copyright 2023 Karl Kegel
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import asyncio
import functools
import gzip
import json
import time
from queue import Queue
from threading import Thread
from typing import Callable
import websockets
from byte_message import ByteMessage
from json_message import JsonMessage
from message import Message
from message_type import MessageType


async def take_coroutine(consumers: set[Callable[[dict], None]], connections: set, websocket):
    try:
        if websocket not in connections:
            print("add to connections " + str(websocket))
        connections.add(websocket)
        async for message in websocket:
            parsed_json = json.loads(message.decode('UTF-8'))
            for consumer in consumers:
                consumer(parsed_json)
    except Exception as e:
        if websocket:
            connections.remove(websocket)


async def send_coroutine(outbox: Queue, connections: set, cycle_time_ms: int):
    while True:
        timestamp_ms = time.time_ns() / 1000_000
        while not outbox.empty():
            message = outbox.get(False)
            print("send message size=", len(message))
            if connections:
                try:
                    for connection in connections:
                        try:
                            await connection.send(message)
                        except Exception as e:
                            print(e)
                            connections.remove(connection)
                except Exception as e:
                    print(e)
            outbox.task_done()
            await asyncio.sleep(0)
        diff = (time.time_ns() / 1000_000) - timestamp_ms
        if 0 <= diff <= cycle_time_ms:
            await asyncio.sleep(diff / 1000)
        else:
            await asyncio.sleep(0)


class IOSocket:
    '''
    A convenient websocekt implementation running async in the backend.
    It takes messages and manages connections with default strategies.
    The main thread is not blocked.
    '''
    __is_open: bool = False

    __consumers: set[Callable[[dict], None]] = None
    __connections: set = None
    __outbox: Queue = None

    HOST: str = None
    PORT: str = None
    GZIP: bool = None
    UPDATE_CYCLE_MS: int = None

    def __init__(self, host: str, port: str, io_cycle_hz: int, gzip_bin=False):
        self.__consumers = set()
        self.__connections = set()
        self.__outbox = Queue()
        self.HOST = host
        self.PORT = port
        self.GZIP = gzip_bin
        self.UPDATE_CYCLE_MS = int((1 / io_cycle_hz) * 1000)

    def register_consumer(self, consumer: Callable[[dict], None]):
        self.__consumers.add(consumer)

    def unregister_consumer(self, consumer: Callable[[dict], None]):
        self.__consumers.remove(consumer)

    def send(self, message: Message):
        if message.message_type == MessageType.BYTES and isinstance(message, ByteMessage):
            if self.GZIP:
                compressed_data = gzip.compress(message.get_data())
                # print("compress large bin: size=", len(message.get_data()), " -> size=", len(compressed_data))
            else:
                compressed_data = message.get_data()
                # print("add uncompressed bin: size=", len(compressed_data))
            self.__outbox.put(compressed_data)
        elif message.message_type == MessageType.JSON and isinstance(message, JsonMessage):
            self.__outbox.put(message.serialize().encode('UTF-8'), False)

    def listen(self):
        if not self.__is_open:
            self.__is_open = True
            t = Thread(target=self.__run)
            t.start()

    def close(self):
        self.__is_open = False

    def __run(self):
        loop = asyncio.new_event_loop()
        asyncio.set_event_loop(loop)
        loop.run_until_complete(
            websockets.serve(functools.partial(take_coroutine, self.__consumers, self.__connections), self.HOST,
                             self.PORT, max_size=2**30))
        loop.create_task(send_coroutine(self.__outbox, self.__connections, self.UPDATE_CYCLE_MS))
        loop.run_forever()
