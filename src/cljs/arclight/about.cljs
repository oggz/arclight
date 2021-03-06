(ns arclight.about)

  (def description "Welcome to Arclight Labs! This website is currently under development. In the near future it will evolve into my personal website; a place to share write-ups on my projects, ideas for future projects, experiments, and more. I will also be experimenting with this website's code which is completely open source and available on github via the link below. It is written completely in Clojure and Clojurescript which are very nice Lisp variants built on the JVM and javascript respectively. They allow for a proper, sane, and unified syntax across the entire project. They also allow for serious performance optimization as the code is compiled to javascript optimized for the google closure (Clojure came first) advanced compiler. Thanks for stopping by and checking out the site!")

(defn about-page [app-state]
  [:div.alpha
   [:h2 "About"]
   [:p description]])
