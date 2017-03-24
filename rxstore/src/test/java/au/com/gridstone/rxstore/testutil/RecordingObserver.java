package au.com.gridstone.rxstore.testutil;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

public final class RecordingObserver<T> implements Observer<T> {
  private final BlockingDeque<Object> events = new LinkedBlockingDeque<Object>();

  @Override public void onComplete() {
    events.addLast(new OnComplete());
  }

  @Override public void onError(Throwable e) {
    events.addLast(new OnError(e));
  }

  @Override public void onSubscribe(Disposable d) {
    events.addLast(new OnSubscribe());
  }

  @Override public void onNext(T t) {
    events.addLast(new OnNext(t));
  }

  private <E> E takeEvent(Class<E> wanted) {
    Object event;
    try {
      event = events.pollFirst(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
    if (event == null) {
      throw new NoSuchElementException(
          "No event found while waiting for " + wanted.getSimpleName());
    }
    assertThat(event).isInstanceOf(wanted);
    return wanted.cast(event);
  }

  public OnSubscribe takeSubscribe() {
    OnSubscribe event = takeEvent(OnSubscribe.class);
    return event;
  }

  public T takeNext() {
    OnNext event = takeEvent(OnNext.class);
    return event.value;
  }

  public Throwable takeError() {
    return takeEvent(OnError.class).throwable;
  }

  public void assertOnCompleted() {
    takeEvent(OnComplete.class);
  }

  public void assertNoMoreEvents() {
    try {
      Object event = takeEvent(Object.class);
      throw new IllegalStateException("Expected no more events but got " + event);
    } catch (NoSuchElementException ignored) {
    }
  }

  private final class OnNext {
    final T value;

    private OnNext(T value) {
      this.value = value;
    }

    @Override public String toString() {
      return "OnNext[" + value + "]";
    }
  }

  private final class OnComplete {
    @Override public String toString() {
      return "OnComplete";
    }
  }

  private final class OnSubscribe {
    @Override public String toString() {
      return "OnSubscribe";
    }
  }

  private final class OnError {
    private final Throwable throwable;

    private OnError(Throwable throwable) {
      this.throwable = throwable;
    }

    @Override public String toString() {
      return "OnError[" + throwable + "]";
    }
  }
}
