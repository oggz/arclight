# Arclight
### A Reactive web app and RESTful web service written completely in **_Clojure_**!

Ring based server. Ring is king! It has the best abstraction I've seen for HTTP. It structures requests and responses as maps of data to pass through layered functions. Once it reaches the core function it is routed by uri and request type, processed by server code, then returns through the functions on the way out in reverse order. This allows for adding middleware anywhere in the process which is essentially a single function. The input and output of this function pipe is handled by rind adapters which translate the map to whatever format the desired server requires. This is a perfect serperation of concerns. If the current server doesn't handle new requirement it can be modularly replaced.

Reagent based reactive single page app. Reagent is a value added abstraction over react.js. It takes advantage of clojures Hash Array Mapped Trie(HAMT) to implement and update the virtual DOM in an optimal way. Though theres no need for the programmer to worry about all that with such a nice abstraction though.

Buddy library for security along with a custom middleware to read the token from a http only secure cookie instead of from an authorization header. Will be ported to a Buddy backend soon. Utilizes JSON Web Tokens to realize a stateless session. More precisely it is client side state but still allows for the servers to scale without need for a large shared mem cache. Only a shared blacklist will be required for token revocation. More work to be done here... SSL will be implemented by default soon.

Any comments about how to improve the code are welcome!

This code is currently under development. Use at your own risk!
