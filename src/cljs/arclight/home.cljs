(ns arclight.home)

(defn home-page [app-state]
  [:div.alpha
   [:h2 "Welcome to Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]
   [:div
    [:p "Welcome to Arclight Labs! This website is currently under development. In the near future it will become my personal website; a place to share writups on my projects, ideas for future projects, experitments, and more. I will likely also be experimenting with this website's code. It is written completely in Clojure and Clojurescript which are very nice Lisp variants built on the JVM and javascript respectively. They allow for a proper, sane, and unified syntax across the entire project. It also allows for some serious performance optimization. Thanks for stopping by!" ]]])
