package org.kin.framework.scala.log

import org.slf4j.{LoggerFactory, Marker}

/**
 * @author huangjianqin
 * @date 2021/1/17
 */
trait LoggerOprs {
  lazy val log = LoggerFactory.getLogger(getClass)

  def name: String = log.getName

  //-------------------------------------------trace-------------------------------------------
  def trace(msg: String): Unit = {
    log.trace(msg)
  }

  def trace(msg: String, arg1: Any): Unit = {
    log.trace(msg, arg1)
  }

  def trace(msg: String, arg1: Any, arg2: Any): Unit = {
    log.trace(msg, arg1, arg2)
  }

  def trace(format: String, args: Any*): Unit = {
    log.trace(format, args)
  }

  def trace(marker: Marker, format: String, args: Any*): Unit = {
    log.trace(marker, format, args)
  }

  def trace(msg: String, t: Throwable): Unit = {
    log.trace(msg, t)
  }

  //-------------------------------------------debug-------------------------------------------
  def debug(msg: String): Unit = {
    log.debug(msg)
  }

  def debug(msg: String, arg1: Any): Unit = {
    log.debug(msg, arg1)
  }

  def debug(msg: String, arg1: Any, arg2: Any): Unit = {
    log.debug(msg, arg1, arg2)
  }

  def debug(format: String, args: Any*): Unit = {
    log.debug(format, args)
  }

  def debug(marker: Marker, format: String, args: Any*): Unit = {
    log.debug(marker, format, args)
  }

  def debug(msg: String, t: Throwable): Unit = {
    log.debug(msg, t)
  }

  def debug(t: Throwable): Unit = {
    log.debug("", t)
  }

  //-------------------------------------------info-------------------------------------------
  def info(msg: String): Unit = {
    log.info(msg)
  }

  def info(msg: String, arg1: Any): Unit = {
    log.info(msg, arg1)
  }

  def info(msg: String, arg1: Any, arg2: Any): Unit = {
    log.info(msg, arg1, arg2)
  }

  def info(format: String, args: Any*): Unit = {
    log.info(format, args)
  }

  def info(marker: Marker, format: String, args: Any*): Unit = {
    log.info(marker, format, args)
  }

  def info(msg: String, t: Throwable): Unit = {
    log.info(msg, t)
  }

  //-------------------------------------------warn-------------------------------------------
  def warn(msg: String): Unit = {
    log.warn(msg)
  }

  def warn(msg: String, arg1: Any): Unit = {
    log.warn(msg, arg1)
  }

  def warn(msg: String, arg1: Any, arg2: Any): Unit = {
    log.warn(msg, arg1, arg2)
  }

  def warn(format: String, args: Any*): Unit = {
    log.warn(format, args)
  }

  def warn(marker: Marker, format: String, args: Any*): Unit = {
    log.warn(marker, format, args)
  }

  def warn(msg: String, t: Throwable): Unit = {
    log.warn(msg, t)
  }

  //-------------------------------------------error-------------------------------------------
  def error(msg: String): Unit = {
    log.error(msg)
  }

  def error(msg: String, arg1: Any): Unit = {
    log.error(msg, arg1)
  }

  def error(msg: String, arg1: Any, arg2: Any): Unit = {
    log.error(msg, arg1, arg2)
  }

  def error(format: String, args: Any*): Unit = {
    log.error(format, args)
  }

  def error(marker: Marker, format: String, args: Any*): Unit = {
    log.error(marker, format, args)
  }

  def error(msg: String, t: Throwable): Unit = {
    log.error(msg, t)
  }
}
