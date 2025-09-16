import { ComponentFixture, TestBed } from '@angular/core/testing';

import { UserGameweekCurrentComponent } from './user-gameweek-current.component';

describe('UserGameweekCurrentComponent', () => {
  let component: UserGameweekCurrentComponent;
  let fixture: ComponentFixture<UserGameweekCurrentComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserGameweekCurrentComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(UserGameweekCurrentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
