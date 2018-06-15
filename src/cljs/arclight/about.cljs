(ns arclight.about)

(defn about-page [app-state]
  [:div.alpha
   [:h2 "About Arclight... " (:counter @app-state)]
   [:button {:on-click #(swap! app-state update-in [:counter] inc)} "Click Me!"]])
