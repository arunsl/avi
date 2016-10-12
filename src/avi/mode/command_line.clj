(ns avi.mode.command-line
  (:require [packthread.core :refer :all]
            [avi.edit-context :as ec]
            [clojure.string :as string]
            [avi.command-line :as cl]
            [avi.commands]
            [avi.editor :as e]
            [avi.pervasive :refer :all]))

(def normal-commands
  {":" (fn+> [editor _]
         (cl/enter :command-line ":"))})

(defn- line-number?
  [command]
  (every? #(Character/isDigit %) command))

(defn- have-exclamation?
  [command]
  (string/includes? command "!"))

(defn- command-fn
  [command-line]
  (ns-resolve 'avi.commands (symbol command-line)))

(defn- process-command
  [editor command-line-raw]
  (let [force? (have-exclamation? command-line-raw)
        command-line (if force?
                    (string/replace command-line-raw "!" "")
                    command-line-raw)]
    (+> editor
      (cond
        (= "" command-line)
        identity

        (line-number? command-line)
        (avi.commands/-NUMBER- command-line)

        (command-fn command-line)
        ((command-fn command-line) force?)

        :else
        (assoc :message [:white :red (str ":" command-line " is not a thing")])))))

(def wrap-mode (cl/mode-middleware :command-line process-command))
