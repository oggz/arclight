(ns arclight.home)

(defn home-page [app-state]
  [:div.alpha
   [:h2 "Welcome to Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]
   [:div
    [:p "Welcome to Arclight Labs! This website is currently under development. In the near future it will evolve into my personal website; a place to share writups on my projects, ideas for future projects, experitments, and more. I will likely also be experimenting with this website's code which is completely open source and available on github via the link below. It is written completely in Clojure and Clojurescript which are very nice Lisp variants built on the JVM and javascript respectively. They allow for a proper, sane, and unified syntax across the entire project. They also allows for some serious performance optimization as the code is compiled to javascript optimized for the google closure (Clojure came first) advanced compiler. Thanks for stopping by and checking out the site!" ]]])
