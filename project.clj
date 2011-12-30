(defproject orc_battle_online "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :dependencies [[org.clojure/clojure "1.2.0"]
		 [org.clojure/clojure-contrib "1.2.0"]
		 [ring "0.3.9"]
		 [hiccup "0.3.4"]
                 [clj-stacktrace "0.2.4"]]
  :dev-dependencies [[lein-ring "0.4.0"]]
  :main orc_battle_online.game_logic
  :ring {:handler orc_battle_online.core/app})
